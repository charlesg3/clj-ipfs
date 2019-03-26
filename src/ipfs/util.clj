(ns ipfs.util
  (:require [clojure.java.io :as io])
  (:import [java.nio.file Files Path FileSystems Paths]))

(defn glob
  [start recursive pattern]
  (let [fs (FileSystems/getDefault)
        matcher (.getPathMatcher fs (str "glob:**/" pattern))
        start (io/file start)
        initial-list (->> start
                          ((fn [f]
                             (if (.isDirectory f)
                               (into [] (.listFiles f))
                               [f]))))]
    (loop [seen #{}
           remain initial-list
           files (if (.isDirectory start)
                   [start]
                   [])]
      (let [current (first remain)
            remain (rest remain)
            path (if current (Paths/get (.toURI current)))]
        (if current
          (if (.isFile current)
            (if (.matches matcher path)
                (recur seen remain (conj files current))
              (recur seen remain files))
            (if recursive
              (recur (conj seen current)
                     (concat remain (into [] (.listFiles current)))
                     (conj files current))
              (recur seen remain files)))
          files)))))
