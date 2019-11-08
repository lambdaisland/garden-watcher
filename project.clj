(defproject lambdaisland/garden-watcher "0.3.4"
  :description "A component for reloading Garden stylesheets"
  :url "https://github.com/lambdaisland/garden-watcher"
  :license {:name "Mozilla Public License 2.0"
            :url "https://www.mozilla.org/en-US/MPL/2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.stuartsierra/component "0.4.0"]
                 [org.clojure/java.classpath "0.3.0"]
                 [hawk "0.2.11"]
                 [garden "1.3.9"]])
