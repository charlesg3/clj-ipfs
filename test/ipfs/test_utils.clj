(ns ipfs.test-utils
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.security MessageDigest]
           [java.io ByteArrayOutputStream]))

(defn delete-recursively [fname]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               (io/delete-file f))]
    (func func (io/file fname))))

(defn file->md5
  [file]
  (let [digester (MessageDigest/getInstance "md5")]
    (->> (with-open [out (ByteArrayOutputStream.)]
           (io/copy (io/input-stream file) out)
           (.toByteArray out))
         (.digest digester)
         (map #(format "%02x" %))
         (apply str))))

(defn dir->file-set
  [directory]
  (->> (file-seq (io/file directory))
       (map (fn [f]
              (if (.isFile f)
                [(str/replace (.getPath f) (re-pattern directory) "")
                 (file->md5 f)])))
       (remove nil?)
       (into #{})))
