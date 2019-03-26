(ns ipfs.client
  (:refer-clojure :exclude [get cat])
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [camel-snake-kebab.core :refer [->kebab-case]]
            [org.httpkit.client :as client]
            [ipfs.tar :as tar]
            [ipfs.util :as util]))


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
  (-> (request (format "/get/%s" multihash))
      (tar/untar-string)))


(defn cat
  [multihash]
  (request (format "/cat/%s" multihash)))


(defn ls
  [multihash]
  (request (format "/ls/%s" multihash) :decoder :json))

(defn version
  []
  (request "/version" :decoder :json))

(defn check-version
  []
  (-> (version)
      (:version)
      (assert-version)))
