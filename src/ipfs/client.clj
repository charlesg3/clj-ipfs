(ns ipfs.client
  (:refer-clojure :exclude [get cat resolve])
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [camel-snake-kebab.core :refer [->kebab-case]]
            [org.httpkit.client :as client]
            [ipfs.tar :as tar]
            [ipfs.util :as util])
  (:import [java.io ByteArrayInputStream]))


(def DEFAULT_HOST "localhost")
(def DEFAULT_PORT 5001)
(def DEFAULT_BASE "api/v0")

(def VERSION_MINIMUM "0.4.3")
(def VERSION_MAXIMUM "0.5.0")

(defn- version-str->version-seq
  [version-str]
  (->> version-str
       ((fn [x] (str/split x #"\.")))
       (mapv #(Integer. ^String %))))

(defn- compare-version
  [compare-op [a-major a-minor a-patch] [b-major b-minor b-patch]]
  (cond
    (and (= a-major b-major) (= a-minor b-minor)) (compare-op a-patch b-patch)
    (and (= a-major b-major)) (compare-op a-minor b-minor)
    :default (compare-op a-patch b-patch)))

(defn- assert-version
  [version-str]
  (let [current-version (version-str->version-seq version-str)
        min-version (version-str->version-seq VERSION_MINIMUM)
        max-version (version-str->version-seq VERSION_MAXIMUM)]
    (if (or (compare-version < current-version min-version)
            (compare-version > current-version max-version))
      (throw (ex-info "Insufficient IPFS Api version detected"
                      {:detected-version version-str
                       :minimum-version VERSION_MINIMUM
                       :maximum-version VERSION_MAXIMUM})))))


(defn- json-decoder
  [input-str]
  (if (and input-str (not= input-str ""))
    (->> (str/split input-str #"\n")
         (mapv #(json/read-str % :key-fn (comp keyword ->kebab-case)))
         ((fn [x]
            (if (= (count x) 1)
              (first x)
              x))))))


(defn- request
  [path & {:keys [decoder method data params]
           :or {decoder :json
                method :get}
           :as opts}]
  (let [decoder (condp = decoder
                  :identity identity
                  :json json-decoder)
        method (condp = method
                 :get client/get
                 :put client/put
                 :post client/post)
        opts (merge (dissoc opts :decoder :method :data :params)
                    (if data {:body data})
                    (if params {:query-params params}))
        url (format "http://%s:%s/%s/%s" DEFAULT_HOST DEFAULT_PORT DEFAULT_BASE path)
        cr #'client/coerce-req]
    ;(println "opts: " opts)
    ;(println (cr (merge {:method :get
    ;                     :url url}
    ;                    opts)))
    (try
    (-> url
        (method opts)
        (deref)
        ((fn [x]
           (if (= (:status x) 200)
             (-> x :body decoder)
             (throw (ex-info "Request Error" x))))))
    (catch Throwable e
      (println "Error:")
      (println e)))))


(defn add
  [start & {:keys [recursive pattern wrap-with-directory]
            :or {recursive false
                 pattern "*"}}]
  (let [files (->> (util/glob start recursive pattern)
                   (mapv (fn [f]
                           (if (.isDirectory f)
                             {:name "files"
                              :content-type "application/x-directory"
                              :content ""
                              :filename (.getPath f)}
                             {:name "files"
                              :filename (.getPath f)
                              :content f}))))]
    (request "add"
             :multipart files
             :params {:wrap-with-directory (if wrap-with-directory "True" "False")}
             :method :post)))


(defn get [multihash & {:keys [output-directory]
                        :or {output-directory "."}}]
  (io/make-parents output-directory)
  (-> (request (format "get/%s" multihash) :decoder :identity)
      (tar/untar-string :output-directory output-directory)))


(defn cat [multihash]
  (request (format "cat/%s" multihash) :decoder :identity))


(defn ls [multihash]
  (request (format "ls/%s" multihash)))


(defn refs [multihash]
  (request (format "refs/%s" multihash)))


(defn refs-local []
  (request "refs/local"))


(defn block-stat [multihash]
  (request (format "block/stat/%s" multihash)))


(defn block-get [multihash]
  (request (format "block/get/%s" multihash) :decoder :identity))


(defn block-put [data]
  (request "block/put"
           :multipart [{:name ""
                        :content data}]
           :method :put))

(defn bitswap-wantlist
  ([] (bitswap-wantlist nil))
  ([peer]
   (if peer
     (request (format "bitswap/wantlist/%s" peer))
     (request "bitswap/wantlist"))))


(defn bitswap-stat []
  (request "bitswap/stat"))


(defn bitswap-unwant [_key]
  (request (format "bitswap/unwant/%s" _key)))


(defn object-data [multihash]
  (request (format "object/data/%s" multihash) :decoder :identity))


(defn object-new
  ([] (object-new nil))
  ([template]
   (if template
     (request (format "/object/new/%s" template))
     (request "object/new"))))


(defn object-links [multihash]
  (request (format "object/links/%s" multihash)))


(defn object-get [multihash]
  (request (format "object/get/%s" multihash :decoder :identity)))


(defn object-put [data]
  (request "object/put"
           :multipart [{:name ""
                        :content data}]
           :method :put))

(defn object-stat [multihash]
  (request (format "object/stat/%s" multihash)))


(defn object-patch-append-data [multihash new-data]
  (request (format "object/patch/append-data")
           :params {:arg multihash}
           :multipart [{:name ""
                        :content new-data}]
           :method :put))


(defn object-patch-add-link [root name ref & {:keys [create]
                                              :or {create false}}]
  (request (format "object/patch/add-link")
           :params {:arg [root name ref]
                    :create (if create "True" "False")}))


(defn object-patch-rm-link [root link]
  (request (format "object/patch/rm-link")
           :params {:arg [root link]}))


(defn object-patch-set-data [root data]
  (request (format "object/patch/set-data")
           :params {:arg [root]}
           :multipart [{:name ""
                        :content data}]
           :method :put))


(defn resolve [name & {:keys [recursive]
                       :or {recursive false}}]
  (request "files/ls" :params {:arg name
                               :recursive (if recursive "True" "False")}))


(defn key-list []
  (request "key/list"))


(defn key-gen [key-name & {:keys [size type]
                            :or {size 2048
                                 type "rsa"}}]
  (request "key/gen"
           :params {:arg key-name
                    :type type
                    :size size}))


(defn key-rm [key-name & key-names]
  (request "key/rm"
           :params {:arg (concat [key-name] key-names)}))


(defn key-rename [key-name new-key-name]
  (request "key/rename"
           :params {:arg [key-name new-key-name]}))


(defn name-publish [ipfs-path & {:keys [resolve lifetime ttl key]
                                 :or {resolve true
                                      lifetime "24h"
                                      ttl nil
                                      key nil}}]
  (request "name/publish"
           :params (merge {:arg ipfs-path
                           :lifetime lifetime
                           :resolve (if resolve "True" "False")}
                          (if key {:key key})
                          (if ttl {:ttl ttl}))))


(defn name-resolve [& {:keys [name recursive nocache]
                       :or {name nil
                            recursive false
                            nocache false}}]
  (request "name/resolve"
           :params (merge {:recursive (if recursive "True" "False")
                           :nocache (if nocache "True" "False")}
                          (if name {:arg name}))))


(defn dns [domain-name & {:keys [recursive]
                          :or {recursive false}}]
  (request "dns" :params {:arg domain-name
                          :recusrive (if recursive "True" "False")}))


(defn pin-add [path & {:keys [recursive extra-paths]}]
  (request "pin/add" :params {:arg (concat [path] extra-paths)
                              :recusrive (if recursive "True" "False")}))


(defn pin-rm [path & {:keys [recursive extra-paths]}]
  (request "pin/rm" :params {:arg (concat [path] extra-paths)
                             :recusrive (if recursive "True" "False")}))


(defn pin-ls [& {:keys [type]
                 :or {type "all"}}]
  (request "pin/ls" :params {:type type}))


(defn pin-update [from-path to-path & {:keys [unpin]}]
  (request "pin/rm" :params (merge {:arg [from-path to-path]}
                                   (if-not (nil? unpin)
                                     {:unpin (if unpin "True" "False")}))))


(defn pin-verify [path & {:keys [verbose extra-paths]}]
  (request "pin/rm" :params {:arg (concat [path] extra-paths)
                             :verbose (if verbose "True" "False")}))


(defn repo-gc []
  (request "/repo/gc"))


(defn repo-stat []
  (request "/repo/stat"))


(defn id
  ([] (id nil))
  ([peer]
   (request (if peer (format "/id/%s" peer) (format "/id")))))


(defn bootstrap-list
  []
  (request "bootstrap"))


(def bootstrap bootstrap-list)


(defn bootstrap-add [peer & peers]
  (request "bootstrap/add" :params {:arg (concat [peer] peers)}))


(defn bootstrap-rm [peer & peers]
  (request "bootstrap/rm" :params {:arg (concat [peer] peers)}))


(defn swarm-peers []
  (request "swarm/peers"))


(defn swarm-addrs []
   (request "swarm/addrs"))


(defn swarm-connect [address & addresses]
   (request "swarm/connect" :params {:arg (concat [address] addresses)}))

(defn swarm-disconnect [address & addresses]
   (request "swarm/disconnect" :params {:arg (concat [address] addresses)}))


(defn swarm-filters-add [address & addresses]
   (request "swarm/filters/add" :params {:arg (concat [address] addresses)}))


(defn swarm-filters-rm [address & addresses]
   (request "swarm/filters/rm" :params {:arg (concat [address] addresses)}))


(defn dht-query [peer-id & peer-ids]
   (request "dht/query" :params {:arg (concat [peer-id] peer-ids)}))


(defn dht-findprovs [multihash & multihashes]
   (request "dht/findprovs" :params {:arg (concat [multihash] multihashes)}))


(defn dht-findpeer [peer-id & peer-ids]
   (request "dht/findpeer" :params {:arg (concat [peer-id] peer-ids)}))


(defn dht-get [key & keys]
   (request "dht/get" :params {:arg (concat [key] keys)}))


(defn dht-put [key value]
   (request "dht/put" :params {:arg (concat [key value])}))


(defn ping [peer & peers]
   (request "/ping" :params {:arg (concat [peer] peers)}))


(defn config [key {:keys [value]
                   :or {value nil}}]
  (request "config" :params {:arg (concat [key]
                                          (if value [value]))}))


(defn config-show []
   (request "config/show"))


(defn config-replace [k v]
   (request "/config/replace" :params {:arg [k v]}))


(defn log-level [subsystem level]
   (request "log/level" :params {:arg [subsystem level]}))


(defn log-ls []
   (request "log/ls"))


(defn log-tail []
   (request "log/tail"))


(defn version []
  (request "version"))


(defn check-version
  []
  (-> (version)
      (:version)
      (assert-version)))

(defn files-ls [path]
  (request "files/ls" :params {:arg path}))


(defn files-cp [source dest]
  (request "files/cp" :params {:arg [source dest]}))


(defn files-mkdir [path]
  (request "files/mkdir" :params {:arg path}))


(defn files-stat [path]
  (request "files/stat" :params {:arg path}))


(defn files-rm [path]
  (request "files/rm" :params {:arg path}))


(defn files-read [path & {:keys [offset count]
                          :or {offset 0}}]
  (request (format "files/read")
           :params (merge {:arg path
                           :offset offset}
                          (if count {:count count}))
           :decoder :identity))


(defn files-write [path data & {:keys [create truncate offset count]
                                :or {create false
                                     truncate false
                                     offset 0}}]
  (request "files/write"
           :multipart [{:name ""
                        :content data}]
           :method :put
           :params (merge {:arg path
                           :offset offset
                           :create (if create "True" "False")
                           :truncate (if truncate "True" "False")}
                          (if count {:count count}))))


(defn files-mv [source dest]
  (request "files/mv"
           :params {:arg [source dest]}))


(defn shutdown []
   (request "shutdown"))


