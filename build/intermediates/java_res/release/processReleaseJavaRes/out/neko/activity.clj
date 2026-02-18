(ns neko.activity
  "Utilities to aid in working with an activity."
  (:require [clojure.string :as s]
            [neko.ui :refer [make-ui]]
            [neko.debug :refer [all-activities safe-for-ui]]
            [neko.internal :as u])
  (:import android.app.Activity
           [android.view View Window]
           android.app.Fragment
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

(defmacro ^{:forms '[name & options & methods]} defactivity
  "Creates an activity with the given full package-qualified name.
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

(defn get-state [^ActivityWithState activity]
  (.getState activity))

(defn simple-fragment
  "Creates a fragment which contains the specified view. If a UI tree
  was provided, it is inflated and then set as fragment's view."
  ([context tree]
     (simple-fragment (make-ui context tree)))
  ([view]
     (proxy [Fragment] []
       (onCreateView [inflater container bundle]
         (if (instance? View view)
           view
           (do
             (println "One-argument version is deprecated. Please use (simple-fragment context tree)")
             (make-ui view)))))))
