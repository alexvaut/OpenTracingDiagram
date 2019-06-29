 (defproject jaeger2diag "0.1.0-SNAPSHOT"
   :description "Convert Jaeger traces into diagrams for Grafana"
   :dependencies [[org.clojure/clojure "1.9.0"]
                  [metosin/compojure-api "2.0.0-alpha30"]
                  [clj-http "3.10.0"]
                  [org.clojure/data.json "0.2.6"]
                  [org.clojure/core.incubator "0.1.4"]
                  [jumblerg/ring.middleware.cors "1.0.1"]
                  ]
   :ring {:handler jaeger2diag.handler/app}
   :uberjar-name "server.jar"
   :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]
                   :plugins [[lein-ring "0.12.5"]]}})
