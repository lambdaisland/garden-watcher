(ns garden-watcher.def
  (:require [garden.core]))

(defmacro defstyles
  "Convenience macro equivalent to `(def name (list styles*))`. Equivalent to
garden.def/defstyles but adds a :garden metadata key if it's not there
already."
  [name & styles]
  `(def ~(vary-meta name #(merge {:garden true} %)) (list ~@styles)))
