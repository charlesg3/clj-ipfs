(ns ipfs.client
  (:refer-clojure :exclude [get cat])
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


(defn json-decoder
  [input-str]
  (->> (str/split input-str #"\n")
       (mapv #(json/read-str % :key-fn (comp keyword ->kebab-case)))
       ((fn [x]
          (if (= (count x) 1)
            (first x)
            x)))))


(defn request
  [path & {:keys [decoder method data params]
           :or {decoder :identity
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
                    (if params {:query-params params}))]

    (-> (format "http://%s:%s/%s/%s" DEFAULT_HOST DEFAULT_PORT DEFAULT_BASE path)
        (method opts)
        (deref)
        (:body)
        decoder)))


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
    (request "/add"
             :multipart files
             :decoder :json
             :params {:wrap-with-directory (if wrap-with-directory "True" "False")}
             :method :post)))


(defn get [multihash & {:keys [output-directory]
                        :or {output-directory "."}}]
  (io/make-parents output-directory)
  (-> (request (format "/get/%s" multihash))
      (tar/untar-string :output-directory output-directory)))


(defn cat [multihash]
  (request (format "/cat/%s" multihash)))


(defn ls [multihash]
  (request (format "/ls/%s" multihash) :decoder :json))


(defn refs [multihash]
  (request (format "/refs/%s" multihash) :decoder :json))


(defn refs-local []
  (request "/refs/local" :decoder :json))


(defn object-new
  ([] (object-new nil))
  ([template]
   (if template
     (request (format "/object/new/%s" template) :decoder :json)
     (request "/object/new/" :decoder :json))))


(defn object-data [multihash]
  (request (format "/object/data/%s" multihash) :decoder :json))


(defn object-links [multihash]
  (request (format "/object/links/%s" multihash) :decoder :json))


(defn object-get [multihash]
  (request (format "/object/get/%s" multihash) :decoder :json))


(defn object-put [data]
  (request "/object/put"
           :multipart [{:name ""
                        :content data}]
           :decoder :json
           :method :put))

(defn block-stat [multihash]
  (request (format "/block/stat/%s" multihash) :decoder :json))


(defn block-get [multihash]
  (request (format "/block/get/%s" multihash)))


(defn block-put [data]
  (request (format "/block/put")
           :multipart [{:name ""
                        :content data}]
           :decoder :json
           :method :put))

(defn version
  []
  (request "/version" :decoder :json))

(defn check-version
  []
  (-> (version)
      (:version)
      (assert-version)))


(defn id
  ([] (id nil))
  ([peer]
   (request (if peer (format "/id/%s" peer) (format "/id")) :decoder :json)))


(defn bootstrap-list
  []
  (request "/bootstrap" :decoder :json))


(def bootstrap bootstrap-list)


(defn bootstrap-list
  []
  (request "/bootstrap" :decoder :json))

