(ns garden-watcher.core
  (:require [clojure.java.io :as io]
            [clojure.java.classpath :as cp]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [garden.core :refer [css]]
            [hawk.core :as hawk])
  (:import java.io.File))

(defn- -ns->path
  "Given a list of namespace symbols, returns a map of namespace symbol -> path of corresponding
   .clj or .cljc file."
  [sym-nses]
  (let [cp-dirs (into #{}
                      (comp (filter #(.isDirectory %))
                            (map #(.getCanonicalPath %)))
                      (concat (cp/classpath (clojure.lang.RT/baseLoader))
                              (cp/system-classpath)))]
    (reduce (fn [ns->paths sym-ns]
              (let [clj-path (str "/"
                                  (-> (str sym-ns)
                                      (str/replace "-" "_")
                                      (str/replace "." File/separator))
                                  ".clj")
                    path     (first (sequence (comp (map (fn [cp-dir]
                                                           (let [canonical-clj-path (str cp-dir
                                                                                         clj-path)]
                                                             (if (.exists (io/file canonical-clj-path))
                                                               canonical-clj-path
                                                               (let [canonical-cljc-path (str canonical-clj-path
                                                                                              "c")]
                                                                 (when (.exists (io/file canonical-cljc-path))
                                                                   canonical-cljc-path))))))
                                                    (filter some?))
                                              cp-dirs))]
                (cond->
                  ns->paths
                  path
                  (assoc sym-ns
                         path))))
            {}
            sym-nses)))

(defn- -reload-and-compile!
  "Reloads the given namespace, then finds all vars with a :garden metadata in that
   namespace, and compiles those to CSS. The target path is either defined in the
   :garden metadata as :output-to, or it iss derived from the var name as
   resources/public/css/<name>.css
  
   Throws when namespace is not found."
  [sym-ns]
  (require sym-ns :reload)
  (doseq [[sym var] (ns-publics sym-ns)]
    (when-let [garden-meta (-> var meta :garden)]
      (let [garden-meta (if (map? garden-meta) garden-meta {})]
        (let [target (:output-to garden-meta (str "resources/public/css/" sym ".css"))]
          (println (str "Garden: compiling #'" sym-ns "/" sym))
          (io/make-parents target)
          (css (assoc garden-meta :output-to target) @var))))))

(defn- -garden-reloader-handler
  "Handler for Hawk which reloads a namespace when its corresponding source file changes."
  [path->ns _ctx event]
  (when (= (:kind event) :modify)
    (-reload-and-compile! (get path->ns
                               (.getCanonicalPath (:file event))))))

(defn compile-garden-namespaces
  "Given a list of namespace symbols, reloads those namespaces, finds all
   definitiions with a :garden metadata key, and compiles them to CSS."
  [sym-nses]
  (run! -reload-and-compile!
        (keys (-ns->path sym-nses))))

(defn start-garden-watcher!
  "Starts a watcher which generates new CSS files when any source file associated with the given
   namespaces changes.
   See `compile-garden-namespaces`."
  [sym-nses hawk-options]
  (let [ns->path (-ns->path sym-nses)]
    (if (seq ns->path)
      (let [paths (vals ns->path)]
        (println "Garden: watching" (str/join ", " paths))
        (run! -reload-and-compile!
              (keys ns->path))
        (hawk/watch! hawk-options
                     [{:handler (partial -garden-reloader-handler
                                         (into {}
                                               (map (fn [[sym-ns path]]
                                                      [path sym-ns]))
                                               ns->path))
                       :paths   paths}]))
      (println "No files found for the given namespaces"))))

(defn stop-garden-watcher! [hawk]
  (hawk/stop! hawk)
  (println "Garden: stopped watching namespaces."))

(defrecord GardenWatcherComponent [namespaces hawk-options]
  component/Lifecycle
  (start [this]
    (if (:garden-watcher-hawk this)
      (do
        (println "Garden: watcher already running.")
        this)
      (assoc this :garden-watcher-hawk (start-garden-watcher! namespaces hawk-options))))
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
   and upon change compiles any definitions with a :garden metadata key to CSS."
  ([namespaces hawk-options]
   (->GardenWatcherComponent namespaces hawk-options))
  ([namespaces]
   (new-garden-watcher namespaces {})))
