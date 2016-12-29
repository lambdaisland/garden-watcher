(ns garden-reloader.main
  (:gen-class)
  (:require [garden-reloader.core :refer [compile-garden-namespaces]]))

(defn -main [& args]
  (compile-garden-namespaces (map symbol args)))
