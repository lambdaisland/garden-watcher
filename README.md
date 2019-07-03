# Garden reloader component

Watch your [Garden](https://github.com/noprompt/garden) stylesheets for changes,
and compiles them to CSS.

## Quickstart

Add `lambdaisland/garden-watcher` as a dependency in `deps.edn` (Clojure CLI),
`project.clj` (Leiningen) or `build.boot` (Boot).

```
;; deps.edn
{lambdaisland/garden-watcher {:mvn/version "0.3.3"}}

;; project.clj/build.boot
[lambdaisland/garden-watcher "0.3.3"]
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

Now you start garden-watcher, passing it your namespace name, and it will
recompile the stylesheet and create a `main.css` file whenever the namespace
changes. You can start garden-watcher in several ways. For more metadata options
(e.g. to control the output file) and a convenience macro (`defstyles`), see
below.

### Use with `garden-watcher.main`

There's a main namespace that's convenient if you want to simply run the watcher
as its own process. You pass one or more namespace names as arguments.

```
;; leiningen
lein run -m garden-watcher.main name.of.your.namespace

;; clojure CLI
clj -m garden-watcher.main name.of.your.namespace
```

### Use as a library

There are start/stop functions so you can hook this up however you like. Useful
for use with Integrant, Mount, etc.

``` clojure
(require '[garden-watcher.core :as gw])

(def watcher (gw/start-garden-watcher! '[name.of.namespace name.of.other.namespace]))
(gw/stop-garden-watcher! watcher)
```

### Use as a Component

If you're using `com.stuartsierra.component` then you can use the built-in
component directly.

Use `garden-watcher.core/new-garden-watcher` to create the component, passing it
a vector of namespace names. Once started this will watch and compile each var
with a `:garden` metadata key in the given namespaces to a CSS file.

``` clojure
(ns user
  (:require [com.stuartsierra.component :as component]
            [figwheel-sidecar.config :as fw-conf]
            [figwheel-sidecar.system :as fw-sys]
            [garden-watcher.core :refer [new-garden-watcher]])) ;; <------

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
garden-watcher you should *not* use `defstylesheet`.

With `defstylesheet` the compile-to-CSS happens as a side effect of loading the
namespace, which makes it impossible to load the namespace without generating
the CSS file, or generating the CSS without reloading the namespace. Therefore
`garden-watcher` chose a different approach.

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

Or you can use `garden-watcher.def/defstyles`, which does the exact same thing,
but automatically adds the `:garden` metadata, so this code is equivalent again:

``` clojure
(require '[garden-watcher.def :refer [defstyles]]) ;; <-- different namespace

(defstyles anja
  [:h1 {:color "blue"}])
```

## One-off Compiling to CSS

To generate CSS files from these vars, use
`garden-watcher.core/compile-garden-namespaces`:

``` clojure
(compile-garden-namespaces '[sesame.styles])
```

garden-watcher also includes a "main" entry point to make it easy to invoke
this as a build step.

```
lein run -m garden-watcher.main sesame.styles
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
                           ["run" "-m" "garden-watcher.main" "sesame.styles"]]
              :omit-source true
              :aot :all}})
```

## License

Copyright © 2016-2019 Arne Brasseur

Distributed under the Mozilla Public License 2.0 (https://www.mozilla.org/en-US/MPL/2.0/)
