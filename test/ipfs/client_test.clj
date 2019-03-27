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

(deftest files-api-test
  (let [random-dir (str "/" (UUID/randomUUID))
        file1-path (str random-dir "/file1")
        file2-path (str random-dir "/file2")
        test-data "test-data"]
    (c/files-mkdir random-dir)
    (c/files-write file1-path test-data :create true)
    (c/files-cp file1-path file2-path)
    (is (= (c/files-read file1-path) test-data))
    (is (= (c/files-read file2-path) test-data))
    (is (= 2 (count (:entries (c/files-ls random-dir)))))
    (println (c/refs-local))
    (c/files-rm file1-path)
    (is (= 1 (count (:entries (c/files-ls random-dir)))))
    (c/files-mv file2-path file1-path)
    (is (= 1 (count (:entries (c/files-ls random-dir)))))
    (is (= (:size (c/files-stat file1-path)) (count test-data)))
    (c/files-rm file1-path)
    (is (= 0 (count (:entries (c/files-ls random-dir)))))))

(deftest refs-test
  (let [{block-key :key} (c/block-put "asdf")
        all-refs (c/refs-local)
        block-ref (->> all-refs
                       (filter #(= (:ref %) block-key))
                       (first))]
    (is (map? block-ref))))

(deftest bitswap-stat-test
  (let [bitswap-stat (c/bitswap-stat)]
    (is (vector? (:peers bitswap-stat)))
    (is (number? (:blocks-received bitswap-stat)))
    (is (number? (:blocks-sent bitswap-stat)))
    (is (number? (:data-received bitswap-stat)))
    (is (number? (:data-sent bitswap-stat)))))


(deftest object-test
  (let [{obj-hash :hash} (c/object-new)
        new-data "new-data"
        {hash-after-set-data :hash}    (c/object-patch-set-data obj-hash new-data)
        {hash-after-link-add :hash}    (c/object-patch-add-link hash-after-set-data "asdf" obj-hash)
        {hash-after-link-rm :hash}     (c/object-patch-rm-link hash-after-link-add "asdf")
        {hash-after-append-data :hash} (c/object-patch-append-data hash-after-link-rm new-data)]
    (is (= (:data (c/object-get hash-after-set-data)) new-data) )
    (is (= (-> (c/object-get hash-after-link-add)
                :links first :name) "asdf"))
    (is (= [] (:links (c/object-get hash-after-link-rm))))
    (is (= (str new-data new-data) (:data (c/object-get hash-after-append-data))))))
