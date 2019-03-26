(ns ipfs.tar
  (:require [clojure.java.io :as io])
  (:import [org.apache.commons.compress.archivers.tar
            TarArchiveInputStream
            TarArchiveOutputStream
            TarArchiveEntry]
           [java.io File FileOutputStream ByteArrayInputStream ByteArrayOutputStream]))

(defn untar-string
  "Untar a string and save to location."
  [input & {:keys [output-directory]
            :or {output-directory "."}}]
  (let [buffer (byte-array 1024)
        outdir (File. output-directory)]
    (.mkdir outdir)
    (with-open [zis (TarArchiveInputStream. (ByteArrayInputStream. (.getBytes input)))]
      (loop [e (.getNextEntry zis)]
        (if e
          (let [filename (.getName e)
                outfile (File. (str outdir (File/separator) filename))]
            (if (.isDirectory e)
              (.mkdirs outfile)
              (do
                (.mkdirs (File. (.getParent outfile)))
                (with-open [outstream (FileOutputStream. outfile)]
                  (loop [len (.read zis buffer)]
                    (if (< 0 len)
                      (do
                        (.write outstream buffer 0 len)
                        (recur (.read zis buffer))))))))
            (recur (.getNextEntry zis))))))))

(defn files->tar
  [files]
  (let [bos (ByteArrayOutputStream.)
        tarfile (TarArchiveOutputStream. bos)]
    (doseq [file files]
      (->> (TarArchiveEntry. file)
           (.putArchiveEntry tarfile))
      (io/copy file tarfile)
      (.closeArchiveEntry tarfile))
    (.close bos)
    bos))
