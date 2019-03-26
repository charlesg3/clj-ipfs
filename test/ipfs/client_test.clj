(ns ipfs.client-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [ipfs.client :as c]
            [ipfs.test-utils :as test-utils])
  (:import [java.util UUID]))

(def readme-multihash "QmPZ9gcCEpqKTo6aq61g2nXGUhM4iCL3ewB6LDXZCtioEB")

(deftest version-test
  (let [r (c/version)]
    (is (:version r))))

(deftest readme-test
  (let [r (c/cat readme-multihash)]
    (is (string? r))))

(deftest block-test
  (let [data "adsf"
        {block-key :key :keys [size]} (c/block-put data)]
    (is (= size (count data)))
    (is (= data (c/block-get block-key)))
    (is (= (count data) (:size (c/block-stat block-key))))))

(deftest add-ls-get-cat-test
  (let [random-dir (str (UUID/randomUUID))
        test-data-dir "test/data/fake_dir"
        upload-result (c/add test-data-dir :recursive true)
        directory-hash (->> upload-result
                            (filter #(= (:name %) "test"))
                            (first)
                            :hash)
        fsdfgh-path "test/data/fake_dir/fsdfgh"
        fsdfgh-hash (->> upload-result
                         (filter #(= (:name %) fsdfgh-path))
                         (first)
                         :hash)
        ls-result (-> directory-hash c/ls :objects first :links first
                      :hash c/ls :objects first :links first :name)
        ;; Download files we just uploaded into a separate directory
        _ (c/get directory-hash :output-directory random-dir)
        source-files (test-utils/dir->file-set "test/data/fake_dir")
        dest-files (test-utils/dir->file-set (format "%s/%s/data/fake_dir" random-dir directory-hash))]
    (= ls-result "fake_dir")
    (is (= (c/cat fsdfgh-hash) (slurp (io/file fsdfgh-path))))
    (is (empty? (set/difference source-files dest-files)))
    (is (empty? (set/difference dest-files source-files)))
    (test-utils/delete-recursively random-dir)))
