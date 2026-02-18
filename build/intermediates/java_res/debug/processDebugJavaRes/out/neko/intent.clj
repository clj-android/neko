(ns neko.intent
  "Utilities to create Intent objects."
  (:require [neko.internal :refer [app-package-name]])
  (:import [android.content Context Intent]
           android.os.Bundle))

(defn put-extras
  "Puts all values from extras-map into the intent's extras. Returns the Intent
  object."
  [^Intent intent, extras-map]
  (doseq [[key value] extras-map
          :let [key (name key)]]
    (condp #(= % (type %2)) value
      ;; Non-reflection calls for the most frequent cases.
      Long (.putExtra intent key ^long value)
      Double (.putExtra intent key ^double value)
      String (.putExtra intent key ^String value)
      Boolean (.putExtra intent key ^boolean value)
      Bundle (.putExtra intent key ^Bundle value)
      ;; Else fall back to reflection
      (.putExtra intent key value)))
  intent)

(defn intent
  "Creates a new Intent object with the supplied extras. In three-arg version
  `classname` can be either a Class or a symbol that will be resolved to a
  class. If symbol starts with dot (like '.MainActivity), application's package
  name will be prenended."
  ([^String action, extras]
   (put-extras (doto (Intent. action)) extras))
  ([^Context context, classname extras]
   (let [^Class class (if (symbol? classname)
                        (resolve
                         (if (.startsWith ^String (str classname) ".")
                           (symbol (str (app-package-name) classname))
                           classname))
                        classname)]
     (put-extras (doto (Intent. context class)) extras))))

