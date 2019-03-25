(ns ipfs.client
  (:refer-clojure :exclude [get cat])
  (:require [clojure.data.json :as json]
            [camel-snake-kebab.core :refer [->kebab-case]]
            [org.httpkit.client :as client]))


(def DEFAULT_HOST "localhost")
(def DEFAULT_PORT 5001)
(def DEFAULT_BASE "api/v0")


(defn request
  [path & {:keys [decoder]
           :or {decoder :identity}}]
  (let [decoder (condp = decoder
                  :identity identity
                  :json #(json/read-str % :key-fn (comp keyword ->kebab-case)))]
    (-> (format "http://%s:%s/%s/%s" DEFAULT_HOST DEFAULT_PORT DEFAULT_BASE path)
        (client/get)
        (deref)
        (:body)
        decoder)))


(defn get
  [multihash]
  (request (format "/get/%s" multihash)))


(defn cat
  [multihash]
  (request (format "/cat/%s" multihash)))


(defn ls
  [multihash]
  (request (format "/ls/%s" multihash) :decoder :json))


(defn version
  []
  (request "/version" :decoder :json))
