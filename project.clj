(defproject baleen "0.1.0-SNAPSHOT"
  :description "Library for writing Event Data Agents"
  :license {:name "The MIT License (MIT)"
            :url "https://opensource.org/licenses/MIT"}
  :aot [baleen.context]
  :plugins [[lein-localrepo "0.5.3"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [yogthos/config "0.8"]
                 [redis.clients/jedis "2.8.0"]
                 [clj-time "0.12.0"]
                 [org.jsoup/jsoup "1.8.3"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.json "0.2.6"]
                 [crossref-util "0.1.10"]
                 [http-kit "2.1.18"]
                 [http-kit.fake "0.2.1"]
                 [liberator "0.14.1"]
                 [compojure "1.5.1"]
                 [ring "1.5.0"]
                 [com.amazonaws/aws-java-sdk "1.11.6"]
                 [com.fasterxml.jackson.core/jackson-databind "2.7.5"]
                 [org.apache.httpcomponents/httpclient "4.5.2"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring/ring-servlet "1.5.0"]
                 [org.eclipse.jetty/jetty-server "9.4.0.M0"]
                 [overtone/at-at "1.2.0"]
                 [robert/bruce "0.8.0"]]

  :profiles {:uberjar {:aot :all}
             :test {:resource-paths ["config/test"]}})
