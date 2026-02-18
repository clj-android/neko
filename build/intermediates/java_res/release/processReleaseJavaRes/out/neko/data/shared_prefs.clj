(ns neko.data.shared-prefs
  "Utilities for interoperating with SharedPreferences class. The original idea
  is by Artur Malabarba."
  (:require [clojure.data :as data])
  (:import [android.content Context SharedPreferences SharedPreferences$Editor]
           neko.App))

(def ^:private sp-access-modes {:private Context/MODE_PRIVATE
                                :world-readable Context/MODE_WORLD_READABLE
                                :world-writeable Context/MODE_WORLD_WRITEABLE})

(defn get-shared-preferences
  "Returns the SharedPreferences object for the given name. Possible modes:
  `:private`, `:world-readable`, `:world-writeable`."
  ([name mode]
   (get-shared-preferences App/instance name mode))
  ([^Context context, name mode]
   {:pre [(or (number? mode) (contains? sp-access-modes mode))]}
   (let [mode (if (number? mode)
                mode (sp-access-modes mode))]
     (.getSharedPreferences context name mode))))

(defn ^SharedPreferences$Editor put
  "Puts the value into the SharedPreferences editor instance. Accepts
  limited number of data types supported by SharedPreferences."
  [^SharedPreferences$Editor sp-editor, key value]
  (let [key (name key)]
    (condp #(= (type %2) %1) value
      java.lang.Boolean (.putBoolean sp-editor key value)
      java.lang.Float  (.putFloat sp-editor key value)
      java.lang.Double (.putFloat sp-editor key (float value))
      java.lang.Integer (.putInt sp-editor key value)
      java.lang.Long    (.putLong sp-editor key value)
      java.lang.String (.putString sp-editor key value)
      ;; else
      (throw (RuntimeException. (str "SharedPreferences doesn't support type: "
                                     (type value)))))))

(defn bind-atom-to-prefs
  "Links an atom and a SharedPreferences file so that whenever the atom is
  modified changes are propagated down to SP. Only private mode is supported to
  avoid inconsistency between the atom and SP."
  [atom prefs-file-name]
  (let [^SharedPreferences sp (get-shared-preferences prefs-file-name :private)]
    (reset! atom (reduce (fn [m [key val]] (assoc m (keyword key) val))
                         {} (.getAll sp)))
    (add-watch atom ::sp-wrapper
               (fn [_ _ old new]
                 (let [^SharedPreferences$Editor editor (.edit sp)
                       [removed added] (data/diff old new)]
                   (doseq [[key _] removed]
                     (.remove editor (name key)))
                   (doseq [[key val] added]
                     (try (put editor key val) (catch RuntimeException ex _)))
                   (.commit editor))))))

(defmacro defpreferences
  "Defines a new atom that will be bound to the given SharedPreferences file.
  The atom can only contain primitive values and strings, and its contents will
  be persisted between application launches. Be aware that if you add an
  unsupported value to the atom it will not be saved which can lead to
  inconsistencies."
  [atom-name prefs-file-name]
  `(do (def ~atom-name (atom {}))
       (when App/instance
         (bind-atom-to-prefs ~atom-name ~prefs-file-name))))
