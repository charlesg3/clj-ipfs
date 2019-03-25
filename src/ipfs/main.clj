(ns ipfs.main
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn exit [status & [msg]]
  (when msg
    (println msg))
  (System/exit status))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn usage [options-summary]
  (->> ["Start the service with no args, otherwise perform maintenance operations."
        ""
        "Usage: java -jar clj-ipfs.jar [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  [none]              Show usage and exit."]
       (str/join \newline)))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args [])
        ;; Support keywords
        arguments (map #(if (and (string? %) (= (first %) \:))
                          (keyword (subs % 1))
                          %)
                       arguments)]
    (if (empty? args)
      (exit 0 (usage summary)))
    ;; Handle help and error
    (cond
      (:help options) (exit 0 (usage summary))
      errors (exit 1 (do (println (usage summary))
                         (error-msg errors))))
    (condp = (keyword (first arguments))
      (exit 1 (usage summary)))
    (exit 0)))
