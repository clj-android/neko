(ns neko.resource
  "Provides utilities to resolve application resources."
  (:require [neko.internal :refer [app-package-name]])
  (:import android.content.Context
           android.graphics.drawable.Drawable
           neko.App))

(defmacro import-all
  "Imports all existing application's R subclasses (R$drawable, R$string etc.)
  into the current namespace."
  []
  `(do ~@(map (fn [res-type]
                `(try (import '~(-> (app-package-name)
                                    (str ".R$" res-type)
                                    symbol))
                      (catch ClassNotFoundException _# nil)))
              '[anim drawable color layout menu string array plurals style id
                dimen raw])))

(import-all)
;; ## Runtime resource resolution

(defn get-string
  "Gets the localized string for the given resource ID. If res-name is a string,
  returns it unchanged. If additional arguments are supplied, the string will be
  interpreted as a format and the arguments will be applied to the format."
  {:forms '([res-id & format-args?] [context res-id & format-args?])}
  [& args]
  (let [[^Context context args] (if (instance? Context (first args))
                                  [(first args) (rest args)]
                                  [App/instance args])
        [res-id & format-args] args]
    (cond (not (number? res-id)) res-id
          format-args      (.getString context res-id (to-array format-args))
          :else            (.getString context res-id))))

(defn get-drawable
  "Gets a Drawable object associated with the given resource ID. If res-id is a
  Drawable, returns it unchanged."
  ([res-id]
   (get-drawable App/instance res-id))

  ([^Context context, res-id]
   (if-not (number? res-id)
     res-id
     (.getDrawable (.getResources context) res-id))))
