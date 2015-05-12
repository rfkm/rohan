(defproject rkworks/rohan "0.1.0-SNAPSHOT"
  :description "Pagination library for Clojure web applications"
  :url "https://github.com/rkworks/rohan"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :deploy-repositories [["snapshots" {:url "https://clojars.org/repo/"
                                      :username [:gpg :env]
                                      :password [:gpg :env]}]
                        ["releases" {:url "https://clojars.org/repo/"
                                     :creds :gpg}]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-codec "1.0.0"]]
  :profiles {:dev    {:dependencies [[midje "1.6.3"]]
                      :plugins      [[lein-midje "3.1.3"]]}
             :1.5    {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6    {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7    {:dependencies [[org.clojure/clojure "1.7.0-beta2"]]}
             :master {:dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]]}}
  :aliases {"all" ["with-profile" "+1.5:+1.6:+1.7:+master"]})
