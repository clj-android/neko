(ns neko.context
  "Utilities to aid in working with a context."
  {:author "Daniel Solano Gómez"}
  (:require [neko.internal :as u])
  (:import android.content.Context
           neko.App))

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
