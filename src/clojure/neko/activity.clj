(ns neko.activity
  "Utilities to aid in working with an activity."
  (:require [clojure.string :as s]
            [neko.ui :refer [make-ui]]
            [neko.debug :refer [all-activities safe-for-ui]]
            [neko.internal :as u])
  (:import android.app.Activity
           android.content.Intent
           [android.view View Window]
           com.goodanser.clj_android.runtime.ClojureActivity
           neko.ActivityWithState))

(defn ^View get-decor-view
  "Returns the root view of the given activity."
  [^Activity activity]
  (.. activity getWindow getDecorView))

(defn set-content-view!
  "Sets the content for the activity.  The view may be one of:

  + neko.ui tree
  + A view object, which will be used directly
  + An integer presumed to be a valid layout ID."
  [^Activity activity, view]
  {:pre [(instance? Activity activity)]}
  (cond
   (instance? View view)
   (.setContentView activity ^View view)

   (integer? view)
   (.setContentView activity ^Integer view)

   :else
   (let [dv (get-decor-view activity)]
     (.setTag dv (java.util.HashMap.))
     (.setContentView activity
                      ^View (neko.ui/make-ui-element activity view
                                                     {:id-holder dv})))))

(defmacro request-window-features!
  "Requests the given features for the activity. The features should be keywords
  such as :no-title or :indeterminate-progress corresponding FEATURE_NO_TITLE
  and FEATURE_INDETERMINATE_PROGRESS, respectively. Returns a sequence of
  booleans whether for each feature that indicates if the feature is supported
  and now enabled.

  This macro should be called before set-content-view!."
  [^Activity activity & features]
  {:pre [(every? keyword? features)]}
  `[~@(for [feat features]
        `(.requestWindowFeature
          ~activity ~(symbol (str (.getName Window) "/FEATURE_"
                                  (u/keyword->static-field (name feat))))))])

(defmacro ^{:forms '[name & options & methods]
             :deprecated "0.5.0"} defactivity
  "DEPRECATED. Use ClojureActivity from runtime-core instead, which
  dynamically delegates lifecycle methods to a Clojure namespace by
  convention — no gen-class or AOT required.

  Creates an activity with the given full package-qualified name.
  Optional arguments should be provided in a key-value fashion.

  Available optional arguments:

  :extends, :implements, :prefix - same as for `gen-class`.

  :features - window features to be requested for the activity.
  Relevant only if :create is used.

  :on-create - takes a two-argument function. Generates a handler for
  activity's `onCreate` event which automatically calls the
  superOnCreate method and creates a var with the name denoted by
  `:def` (or activity's lower-cased name by default) to store the
  activity object. Then calls the provided function onto the
  Application object.

  :on-start, :on-restart, :on-resume, :on-pause, :on-stop, :on-destroy
  - same as :on-create but require a one-argument function."
  [name & args]
  (if (some #{:on-create} args)
    (throw
     (RuntimeException.
      (str "ERROR: This syntax of defactivity is deprecated, please "
           "update it to the new syntax: "
           "https://github.com/clojure-android/neko/wiki/Namespaces#defining-an-activity")))
    (let [[{:keys [extends implements prefix state key features]} methods]
          (loop [args args, options {}, methods {}]
            (cond (empty? args) [options methods]

                  (keyword? (first args))
                  (recur (drop 2 args)
                         (assoc options (first args) (second args))
                         methods)

                  :else
                  (recur (rest args) options
                         (assoc methods (ffirst args) (first args)))))

          sname (u/simple-name name)
          prefix (or prefix (str sname "-"))
          extends (resolve (or extends 'android.app.Activity))
          state (or state `(atom {}))
          release-build? (:neko.init/release-build *compiler-options*)
          exposed-methods (if release-build?
                            (map str (keys methods))
                            (u/list-all-methods extends))]
      `(do
         (gen-class
          :name ~name
          :main false
          :prefix ~prefix
          :init "init"
          :state "state"
          :extends ~(symbol (.getName extends))
          :implements ~(conj implements neko.ActivityWithState)
          :overrides-methods ~(when release-build?
                                (map (fn [[_ [mname args]]] [mname (count args)])
                                     (assoc methods 'getState '(getState [this]))))
          :exposes-methods
          ~(->> exposed-methods
                distinct
                (map (fn [mname]
                       [(symbol mname) (symbol (str "super" (u/capitalize mname)))]))
                (into {})))

         ~`(defn ~(symbol (str prefix "init"))
             [] [[] ~state])
         ~`(defn ~(symbol (str prefix "getState"))
             [~(vary-meta 'this assoc :tag name)]
             (.state ~'this))
         ~(when-let [[mname args & body] (get methods 'onCreate)]
            `(defn ~(symbol (str prefix mname))
               [~(vary-meta (first args) assoc :tag name)
                ~(vary-meta (second args) assoc :tag android.os.Bundle)]
               (.put all-activities '~(.name *ns*) ~'this)
               ~(when key
                  `(.put all-activities ~key ~'this))
               ~(when features
                  `(request-window-features! ~'this ~@features))
               (safe-for-ui ~@body)))
         ~@(for [[_ [mname args & body]] (dissoc methods 'onCreate)]
             `(defn ~(symbol (str prefix mname))
                [~(vary-meta (first args) assoc :tag name)
                 ~@(rest args)]
                (safe-for-ui ~@body)))))))

(defn start-activity-for-result
  "Launches an activity for result with automatic request-code management.

  `activity` must be a ClojureActivity instance. `action-or-intent` is either
  an Intent object or a string action (e.g. Intent/ACTION_OPEN_DOCUMENT_TREE).

  Options (as trailing keyword args):
    :on-result  (fn [activity result-code intent]) — called when result arrives
                with RESULT_OK. Required.
    :on-cancel  (fn [activity]) — called when result is not OK. Optional.
    :type       MIME type string (e.g. \"image/*\"). Only used when
                action-or-intent is a string action.
    :category   Category string to add to the intent. Only used when
                action-or-intent is a string action.

  Request codes 0–9999 are reserved for manual use with on-activity-result.
  Auto-generated codes start at 10000.

  Example:
    (start-activity-for-result activity Intent/ACTION_OPEN_DOCUMENT_TREE
      :on-result (fn [activity result-code data]
                   (let [uri (.getData data)]
                     (swap! prefs* assoc :directory (str uri)))))"
  [^ClojureActivity activity action-or-intent & {:keys [on-result on-cancel type category]}]
  {:pre [(instance? ClojureActivity activity)
         (some? on-result)]}
  (let [^Intent intent (if (instance? Intent action-or-intent)
                         action-or-intent
                         (cond-> (Intent. ^String action-or-intent)
                           type     (.setType type)
                           category (.addCategory category)))
        code (.registerResultCallback activity on-result on-cancel)]
    (.startActivityForResult activity intent code)))

(defn start-activity
  "Launches an activity without expecting a result.

  `activity` must be an Activity instance. `action-or-intent` is either
  an Intent object or a string action.

  Options (as trailing keyword args):
    :type       MIME type string (e.g. \"image/*\"). Only used when
                action-or-intent is a string action.
    :category   Category string to add to the intent. Only used when
                action-or-intent is a string action.
    :extras     Map of string keys to values, added via .putExtra.
    :data       URI to set on the intent via .setData.
    :flags      Integer flags to set on the intent via .setFlags.

  Example:
    (start-activity activity Intent/ACTION_VIEW
      :data (Uri/parse \"https://example.com\"))"
  [^Activity activity action-or-intent & {:keys [type category extras data flags]}]
  {:pre [(instance? Activity activity)]}
  (let [^Intent intent (if (instance? Intent action-or-intent)
                         action-or-intent
                         (cond-> (Intent. ^String action-or-intent)
                           type     (.setType type)
                           category (.addCategory category)
                           data     (.setData data)
                           flags    (.setFlags (int flags))))]
    (when (and extras (not (instance? Intent action-or-intent)))
      (doseq [[k v] extras]
        (.putExtra intent ^String k ^String (str v))))
    (.startActivity activity intent)))

(defn request-permissions
  "Requests runtime permissions with automatic request-code management.

  `activity` must be a ClojureActivity instance. `permissions` is a
  collection of permission strings (e.g. [Manifest$permission/CAMERA]).

  The callback receives (activity permissions grant-results) where
  grant-results is an int array of PackageManager/PERMISSION_GRANTED
  or PERMISSION_DENIED values.

  Request codes 0–9999 are reserved for manual use with
  on-request-permissions-result. Auto-generated codes start at 10000.

  Example:
    (request-permissions activity
      [Manifest$permission/CAMERA
       Manifest$permission/RECORD_AUDIO]
      (fn [activity permissions grant-results]
        (when (every? #(= % PackageManager/PERMISSION_GRANTED)
                      grant-results)
          (start-recording!))))"
  [^ClojureActivity activity permissions callback]
  {:pre [(instance? ClojureActivity activity)
         (some? callback)
         (seq permissions)]}
  (let [perms (into-array String permissions)
        code  (.registerPermissionCallback activity callback)]
    (.requestPermissions activity perms code)))

(defn get-state [^ActivityWithState activity]
  (.getState activity))
