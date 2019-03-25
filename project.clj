(defproject clj-ipfs "0.1.0-SNAPSHOT"
  :description "A client library for the IPFS protocol"
  :url "http://github.com/charlesg3/clj-ipfs"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/mit-license.php"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.cli "0.4.1"]

                 [camel-snake-kebab "0.4.0"]

                 [http-kit "2.3.0"]
                 [org.slf4j/slf4j-nop "1.7.25"]
                 [org.clojure/data.json "0.2.6"]]

  :repl-options {:init-ns ipfs.main}
  :main ^:skip-aot ipfs.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :prep-tasks ["javac"
                                    "compile"]}
             :dev  {:source-paths ["env/dev/"]}}
  :clean-targets ^{:protect false} [:target-path
                                    ".nrepl-port"]
  :uberjar-name "clj-ipfs.jar")


