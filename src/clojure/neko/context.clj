(ns neko.context
  "Utilities to aid in working with a context."
  {:author "Daniel Solano Gómez"}
  (:require [neko.internal :as u])
  (:import android.content.Context
           neko.App))

(defn set-app-context!
  "Sets the global application context used by neko's convenience functions.
  Call this early in your app's initialization (e.g., from your Activity's
  onCreate) if your Application class is not neko.App."
  [^android.app.Application app]
  (set! App/instance app))

(defmacro get-service
  "Gets a system service for the given type. Type is a keyword that names the
  service. Examples include :alarm for the alarm service and
  :layout-inflater for the layout inflater service."
  {:pre [(keyword? type)]}
  ([type]
   `(get-service neko.App/instance ~type))
  ([context type]
   `(.getSystemService
     ^Context ~context
     ~(symbol (str (.getName Context) "/"
                   (u/keyword->static-field (name type)) "_SERVICE")))))
