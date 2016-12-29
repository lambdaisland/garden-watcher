# Garden reloader component

A "[Sierra Component](https://github.com/stuartsierra/component)" that watches
your [Garden](https://github.com/noprompt/garden) stylesheets for changes, and
compiles them to CSS.

## Quickstart

Add `lambdaisland/garden-reloader` as a dependency in `project.clj` (Leiningen)
or `build.boot` (Boot).

```
[lambdaisland/garden-reloader "0.1.0"]
```

Create vars containing Garden-style declarations, and add a `^:garden` metadata
to the var.

``` clojure
(ns sesame.styles)

(def ^:garden main
  (list
   [:h1 {:color "blue"}]
   [:h2 {:color "green"}]
   [:h3 {:color "red"}]))
```

Now use `garden-reloader.core/new-garden-watcher`, passing it a vector of namespace
names, to get a component that will watch and compile each var with a `:garden`
metadata key in the given namespaces to a CSS file.

``` clojure
(ns user
  (:require [com.stuartsierra.component :as component]
            [figwheel-sidecar.config :as fw-conf]
            [figwheel-sidecar.system :as fw-sys]
            [garden-reloader.core :refer [new-garden-watcher]])) ;; <------

(defn dev-system []
  (component/system-map
   :figwheel-system (fw-sys/figwheel-system (fw-conf/fetch-config))
   :css-watcher (fw-sys/css-watcher {:watch-paths ["resources/public/css"]})
   :garden-watcher (new-garden-watcher '[sesame.styles]))) ;; <------
```

The above will generate `resources/public/css/main.css`, and recreate it
whenever `styles.clj` is saved. By combining it with Figwheel's CSS reloader you
get instant reloading in the browser.

## Use of metadata

Garden provides a macro, `garden.def/defstylesheet`, that allows you to pass
compiler options to `garde.core/css`. By adding `:output-to` this allows writing
stylesheets that "automatically" create the corresponding CSS files. When using
garden-reloader you should *not* use `defstylesheet`.

With `defstylesheet` the compile-to-CSS happens as a side effect of loading the
namespace, which makes it impossible to load the namespace without generating
the CSS file, or generating the CSS without reloading the namespace. Therefore
`garden-reloader` chose a different approach.

Instead create regular vars, adn tag them with a `:garden` metadata key. You can
use a map to add compiler options, including `:output-to`.

``` clojure
(def
  ^{:garden {:output-to "resources/public/styles.css"
             :vendors ["webkit"]}}
  my-stylesheet
  (list
   [:h1 {:color "blue"}]))
```

If you don't specify an output file, it defaults to `resources/public/css/{{var-name}}.css`

``` clojure
;; This creates resources/public/css/anja.css
(def ^:garden anja
  (list
   [:h1 {:color "blue"}]))
```

Note that Garden provides `garden.def/defstyles` macro that allows you to get
rid of the `(list ,,,)` (that's all that macro does), so this code is equivalent:

``` clojure
(require '[garden.def :refer [defstyles]])

(defstyles ^:garden anja
  [:h1 {:color "blue"}])
```

Or you can use `garden-reloader.def/defstyles`, which does the exact same thing,
but automatically adds the `:garden` metadata, so this code is equivalent again:

``` clojure
(require '[garden-reloader.def :refer [defstyles]]) ;; <-- different namespace

(defstyles anja
  [:h1 {:color "blue"}])
```

## One-off Compiling to CSS

To generate CSS files from these vars, use
`garden-reloader.core/compile-garden-namespaces`:

``` clojure
(compile-garden-namespaces '[sesame.styles])
```

garden-reloader also includes a "main" entry point to make it easy to invoke
this as a build step.

```
lein run -m garden-reloader.main sesame.styles
```

E.g. say you're building an uberjar containing compiled ClojureScript and CSS.

``` clojure
(defproject sesame "0.1.0"
  ,,,
  :uberjar-name "sesame.jar"

  :profiles {,,,

             :uberjar
             {:prep-tasks ["compile"
                           ["cljsbuild" "once" "min"]
                           ["run" "-m" "garden-reloader.main" "sesame.styles"]]
              :omit-source true
              :aot :all}})
```

## Watching for changes

`garden-reloader.core/new-garden-watcher` creates a component that, upon
starting, reloads the given namespaces and generates CSS files for all vars with
`:garden` metadata, and then watches the filesystem for changes, recompiling the
CSS whenever a namespace is saved.

## License

Copyright © 2016 Arne Brasseur

Distributed under the Mozilla Public License 2.0 (https://www.mozilla.org/en-US/MPL/2.0/)
