(ns garden-watcher.main
  (:gen-class)
  (:require [garden-watcher.core :refer [compile-garden-namespaces]]
            [clojure.tools.cli :refer [parse-opts]]))

(def ^:private cli-options
  ;; An option with a required argument
  [["-n" "--namespace NAMESPACE" "Namespace (can be specified multiple times)"
    :parse-fn #(symbol %)
    :assoc-fn (fn [m k v] (println m k v)
                (let [coll (or (:namespaces m) [])]
                  (assoc m :namespaces (conj coll v))))]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn -main [& args]
  (let [parsed-opts (parse-opts args cli-options)]
    (if-let [help (-> parsed-opts :options :help)]
      (println (str "Syntax: lein run -m garden-watcher.main -n <namespace>\n\nOptions:\n\n"
                    (:summary parsed-opts)))
      (compile-garden-namespaces (-> parsed-opts :options :namespaces)))))
