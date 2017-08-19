(ns garden-watcher.core
  (:require [clojure.java.io :as io]
            [clojure.java.classpath :as cp]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [garden.core :refer [css]]
            [hawk.core :as hawk])
  (:import java.io.File
           java.nio.file.Paths
           java.nio.file.Files))

(defn- ns->file-name
  "Copied from clojure.tools.namespace.move because it's private there."
  [sym]
  (str (-> (name sym)
           (str/replace "-" "_")
           (str/replace "." File/separator))
       ".clj"))

(defn- file-name->ns
  "Converts a relative file name (string) into a namespace."
  [file]
  (-> file
      (str/replace ".clj" "")
      (str/replace File/separator ".")
      (str/replace "_" "-")
      symbol))

(defn- sanitize-classpath
  "Strips any classpath from the file path."
  [file-path]
  (reduce #(str/replace %1 (re-pattern (str "^" (.getPath %2))) "")
          file-path
          (cp/classpath-directories)))

(defn- sanitize-leading-sep
  "Strips any leading separator from a file path."
  [file-path]
  (str/replace file-path (re-pattern (str "^" File/separator)) ""))

(defn- file-path->relative-path
  "Converts a full file path to a relative file path."
  [file-path]
  (-> file-path
      sanitize-classpath
      sanitize-leading-sep))

(defn- file-on-classpath
  "Given a relative path to a source file, find it on the classpath, returning a
  fully qualified java.io.File "
  [path]
  (->> (cp/classpath)
       (map #(io/file % path))
       (filter #(.exists %))
       first))

(defn- reload-and-compile!
  "Reload the given path, then find all vars with a :garden metadata in the
  corresponding namespace, and compile those to CSS. The target path is either
  defined in the :garden metadata as :output-to, or it's derived from the var
  name as resources/public/css/<name>.css"
  [path]
  (let [ns (-> path file-path->relative-path file-name->ns)]
    (require ns :reload-all)
    (doseq [[sym var] (ns-publics ns)]
      (when-let [garden-meta (-> var meta :garden)]
        (let [garden-meta (if (map? garden-meta) garden-meta {})]
          (let [target (:output-to garden-meta (str "resources/public/css/" sym ".css"))]
            (println (str "Garden: compiling #'" ns "/" sym))
            (io/make-parents target)
            (css (assoc garden-meta :output-to target) @var)))))))

(defn- garden-reloader-handler [master-ns]
  (fn [_ctx event]
    (when (= (:kind event) :modify)
      (reload-and-compile! (str (:file event)))
      (when master-ns
        (when-let [master-file (-> master-ns ns->file-name file-on-classpath)]
          (reload-and-compile! (.getPath master-file)))))))

(defn- watch-path [entry]
  (cond
    (symbol? entry) (-> entry ns->file-name file-on-classpath)
    (string? entry) (io/as-file (str (System/getProperty "user.dir")
                                     File/separator
                                     entry))))

(defn master-ns-and-watch->namespaces
  "Given a master namespace and a watch list, returns a list of namespaces"
  [master-ns watch]
  (->> watch
       (map watch-path)
       (#(if master-ns (conj % (-> master-ns ns->file-name file-on-classpath)) %))
       (filter #(.isFile %))))

(defn compile-garden-paths
  "Given a list of paths, reloads the each, finds all syms with a :garden metadata
  key, and compiles them to CSS."
  [paths]
  (run! #(reload-and-compile! %)
        paths))

(defn compile-garden-namespaces
  "Given a list of paths, reloads the each, finds all syms with a :garden metadata
  key, and compiles them to CSS."
  [namespaces]
  (compile-garden-paths (map watch-path namespaces)))

(defrecord GardenWatcherComponent [master-ns watch]
  component/Lifecycle
  (start [this]
    (if (:garden-watcher-hawk this)
      (do
        (println "Garden: watcher already running.")
        this)
      (let [paths (map #(-> % watch-path .getPath) watch)
            handler (garden-reloader-handler master-ns)]

        (compile-garden-paths (master-ns-and-watch->namespaces master-ns watch))
        (println "Garden: watching" (str/join ", " paths))

        (let [watcher (hawk/watch! [{:paths (vec paths)
                                     :handler handler}])]
          (assoc this :garden-watcher-hawk watcher)))))
  
  (stop [this]
    (if-let [hawk (:garden-watcher-hawk this)]
      (do
        (hawk/stop! hawk)
        (println "Garden: stopped watching namespaces.")
        (dissoc this :garden-watcher-hawk))
      (do
        (println "Garden: watcher not running")
        this))))

(defn new-garden-watcher
  "Create a new Sierra Component that watches the given namespaces and/or paths for changes,
  and upon change compiles any symbols with a :garden metadata key to CSS.

  Watchers are set using the `:watch` keyword and specifying namespaces (as symbols) or paths
  (as strings).

  Alternatevely, a `:master-ns` can be specified. This is a symbol pointing to a master namespace
  that needs to be regenerated shall any change occur."
  [{:keys [master-ns watch]}]
  (->GardenWatcherComponent master-ns watch))
