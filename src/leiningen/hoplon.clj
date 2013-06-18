(ns leiningen.hoplon
  (:import
    [java.util.jar JarFile]
    [java.util.zip ZipFile])
  (:require
    [clojure.java.io                    :refer [file input-stream make-parents]]
    [tailrecursion.hoplon.compiler.file :as f]
    [tailrecursion.hoplon.compiler.core :as hl]
    [leiningen.core.eval                :as le]))

(defn deep-merge-with [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

(def default-opts
  {:html-src      "src/html"
   :static-src    "src/static"
   :include-src   "src/include"
   :cljs-src      "src/cljs"
   :work-dir      ".hoplon-work-dir"
   :html-out      "resources/public"
   :outdir-out    "out"
   :pre-script    "pre-compile"
   :post-script   "post-compile"
   :pretty-print  false
   :includes      []
   :cljsc-opts    {:externs []}})

(defn process-opts [opts]
  (let [opts (deep-merge-with #(last %&) default-opts opts)
        work #(.getPath (apply file (:work-dir opts) %&))]
    (assoc opts :html-work     (work "html")
                :cljs-work     (work "cljs")
                :cljs-stage    (work "cljs-stage")
                :out-work      (work "out")
                :stage-work    (work "stage")
                :include-work  (work "include")
                :inc-dep       (work "dep" "inc")
                :lib-dep       (work "dep" "lib")
                :flib-dep      (work "dep" "flib")
                :ext-dep       (work "dep" "ext")
                :cljs-dep      (work "dep" "cljs"))))

(defn start-compiler [project auto]
  (let [lockf ".hoplon-lock"
        opts    (process-opts (:hoplon project {}))
        start   #(hl/start project opts :auto auto)
        errfmt  "Hoplon compiler already running in JVM '%s'."
        lockerr #(println (format errfmt (slurp lockf)))]
    (if (f/lockfile lockf) (start) (lockerr))))
  
(def subtasks
  {nil    #(start-compiler % false)
   :auto  #(start-compiler % true)})

(defn hoplon
  "Hoplon compiler.
  
  USAGE: lein hoplon
  Compile once.
  
  USAGE: lein hoplon auto
  Watch source dirs and compile when necessary."
  [project & [subtask & _]]
  (if-let [f (subtasks (keyword subtask))]
    (f project)
    (throw (Exception. (format "Unknown subtask: '%s'" subtask)))))