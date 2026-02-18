(ns neko.tools.repl
  (:require [neko.log :as log])
  (:import android.content.Context
           android.util.Log
           java.io.FileNotFoundException
           java.util.concurrent.atomic.AtomicLong
           java.util.concurrent.ThreadFactory))

(defn android-thread-factory
  "Returns a new ThreadFactory with increased stack size. It is used to
  substitute nREPL's native `configure-thread-factory` on Android platform."
  []
  (let [counter (AtomicLong. 0)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (doto (Thread. (.getThreadGroup (Thread/currentThread))
                       runnable
                       (format "nREPL-worker-%s" (.getAndIncrement counter))
                       1048576) ;; Hardcoded stack size of 1Mb
          (.setDaemon true))))))

(defn- patch-unsupported-dependencies
  "Some non-critical CIDER and nREPL dependencies cannot be used on Android
  as-is, so they have to be tranquilized."
  []
  (let [curr-ns (ns-name *ns*)]
    (ns dynapath.util)
    (defn add-classpath! [& _])
    (defn addable-classpath [& _])
    (in-ns curr-ns)))

(defn enable-compliment-sources
  "Initializes compliment sources if their namespaces are present."
  []
  (try (require 'neko.compliment.ui-widgets-and-attributes)
       ((resolve 'neko.compliment.ui-widgets-and-attributes/init-source))
       (catch Exception ex nil)))

(defn start-repl
  "Starts a remote nREPL server. Creates a `user` namespace because nREPL
  expects it to be there while initializing. References nrepl's `start-server`
  function on demand because the project can be compiled without nrepl
  dependency."
  [& repl-args]
  (binding [*ns* (create-ns 'user)]
    (refer-clojure)
    (patch-unsupported-dependencies)
    (require 'nrepl.server)
    (let [start-server (resolve 'nrepl.server/start-server)]
      (apply start-server repl-args))))

(defn start-nrepl-server
  "Starts an nREPL server on the given port (default 9999).
  Call from Application.onCreate or Activity.onCreate."
  [& {:keys [port] :or {port 9999} :as args}]
  (try (apply start-repl (mapcat identity (assoc args :port port)))
       (log/i "nREPL started at port" port)
       (catch Exception ex
         (log/e "Failed to start nREPL" :exception ex))))

(defn init
  "Entry point to neko.tools.repl namespace from Java code."
  [& {:as args}]
  (apply start-nrepl-server (mapcat identity args)))
