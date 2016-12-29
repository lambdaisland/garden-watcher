(ns garden-watcher.main
  (:gen-class)
  (:require [garden-watcher.core :refer [compile-garden-namespaces]]))

(defn -main [& args]
  (compile-garden-namespaces (map symbol args)))
