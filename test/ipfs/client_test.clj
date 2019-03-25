(ns ipfs.client-test
  (:require [clojure.test :refer [deftest is]]
            [ipfs.client :as c]))

(def readme-multihash "QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB")

(deftest version-test
  (let [r (c/version)]
    (is (:version r))))

(deftest readme-test
  (let [r (c/cat readme-multihash)]
    (is (string? r))))
