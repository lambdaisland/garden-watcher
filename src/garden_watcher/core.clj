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

(defn- ns-file-name
  "Copied from clojure.tools.namespace.move because it's private there."
  [sym]
  (str (-> (name sym)
           (str/replace "-" "_")
           (str/replace "." File/separator))
       ".clj"))

(defn- file-on-classpath
  "Given a relative path to a source file, find it on the classpath, returning a
fully qualified java.io.File "
  [path]
  (->> (cp/classpath)
       (map #(io/file % path))
       (filter #(.exists %))
       first))

(defn- select-ns-path
  "Given a list of namespace names (symbols) and a path (string), transforms the
path so it's relative to the classpath"
  [namespaces file]
  (let [ns-paths (map ns-file-name namespaces)]
    (first (filter #(.endsWith file %) ns-paths))))

(defn- file->ns
  "Given a list of namespace names (symbols) and a path (string), return the
namespace name that corresponds with the path name"
  [namespaces path]
  (first (filter #(.endsWith path (ns-file-name %)) namespaces)))

(defn- reload-and-compile!
  "Reload the given path, then find all vars with a :garden metadata in the
corresponding namespace, and compile those to CSS. The target path is either
defined in the :garden metadata as :output-to, or it's derived from the var
name as resources/public/css/<name>.css"
  [namespaces path]
  (when-let [ns (file->ns namespaces path)]
    (require ns :reload)
    (doseq [[sym var] (ns-publics ns)]
      (when-let [garden-meta (-> var meta :garden)]
        (let [garden-meta (if (map? garden-meta) garden-meta {})]
          (let [target (:output-to garden-meta (str "resources/public/css/" sym ".css"))]
            (println (str "Garden: compiling #'" ns "/" sym))
            (io/make-parents target)
            (css (assoc garden-meta :output-to target) @var)))))))

(defn- garden-reloader-handler [namespaces]
  (fn [_ctx event]
    (when (= (:kind event) :modify)
      (when-let [ns-path (select-ns-path namespaces (str (:file event)))]
        (reload-and-compile! namespaces ns-path)))))

(defn compile-garden-namespaces
  "Given a list of namespaces (seq of symbol), reloads the namespaces, finds all
  syms with a :garden metadata key, and compiles them to CSS."
  [namespaces]
  (run! #(reload-and-compile! namespaces %)
        (map ns-file-name namespaces)))

(defn start-garden-watcher! [namespaces]
  (let [paths   (map (comp file-on-classpath ns-file-name) namespaces)
        handler (garden-reloader-handler namespaces)]
    (compile-garden-namespaces namespaces)
    (println "Garden: watching" (str/join ", " paths))
    (hawk/watch! [{:paths paths :handler handler}])))

(defn stop-garden-watcher! [hawk]
  (hawk/stop! hawk)
  (println "Garden: stopped watching namespaces."))

(defrecord GardenWatcherComponent [namespaces]
  component/Lifecycle
  (start [this]
    (if (:garden-watcher-hawk this)
      (do
        (println "Garden: watcher already running.")
        this)
      (assoc this :garden-watcher-hawk (start-garden-watcher! namespaces))))
  (stop [this]
    (if-let [hawk (:garden-watcher-hawk this)]
      (do
        (stop-garden-watcher! hawk)
        (dissoc this :garden-watcher-hawk))
      (do
        (println "Garden: watcher not running")
        this))))

(defn new-garden-watcher
  "Create a new Sierra Component that watches the given namespaces for changes,
and upon change compiles any symbols with a :garden metadata key to CSS."
 [namespaces]
  (->GardenWatcherComponent namespaces))
