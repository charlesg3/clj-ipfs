(ns ipfs.tar
  (:import [org.apache.commons.compress.archivers.tar TarArchiveInputStream]
           [java.io File FileOutputStream ByteArrayInputStream]))

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
