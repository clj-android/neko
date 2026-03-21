(ns neko.ui
  "Tools for defining and manipulating Android UI elements."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [apply-trait]]
            [neko.internal :refer [keyword->setter reflect-setter
                                 reflect-constructor]]
            [neko.threading :refer [on-ui]]
            [neko.util :refer [edit-distance]]
            [clojure.string :as string])
  (:import android.content.res.Configuration
           neko.App
           java.lang.ref.WeakReference))

;; ## Reactive cell detection (optional, activated when neko.reactive is loaded)

(defonce ^:private cell-class-cache (atom nil))

(defn- cell-class
  "Returns the neko.reactive.Cell class if loaded, nil otherwise."
  []
  (or @cell-class-cache
      (try
        (let [c (Class/forName "neko.reactive.Cell")]
          (reset! cell-class-cache c)
          c)
        (catch ClassNotFoundException _ nil))))

(defn- extract-cells
  "If cell-cls is non-nil, scans attributes for reactive cell values.
  Returns [resolved-attrs bindings] where resolved-attrs has cells
  replaced with their current (derefed) values, and bindings is a
  vector of [attr-key cell] pairs for watch setup."
  [attributes cell-cls]
  (if cell-cls
    (reduce-kv
      (fn [[attrs binds] k v]
        (if (instance? cell-cls v)
          [(assoc attrs k @v) (conj binds [k v])]
          [attrs binds]))
      [attributes []]
      attributes)
    [attributes nil]))

;; ## Error reporting

(defn- suggest-attributes
  "Returns up to 3 known attribute names similar to `attr-kw` for
  the given widget keyword, sorted by edit distance."
  [widget-kw attr-kw]
  (let [attr-name (name attr-kw)
        all-traits (kw/all-traits widget-kw)
        ;; Gather trait attributes from apply-trait metadata
        trait-attrs (into #{} (mapcat (fn [t]
                                        (get-in (meta #'apply-trait)
                                                [:attributes t] [t]))
                                      all-traits))
        candidates (map name trait-attrs)]
    (->> candidates
         (map (fn [c] {:name c :dist (edit-distance attr-name c)}))
         (filter #(<= (:dist %) (max 3 (quot (count attr-name) 2))))
         (sort-by :dist)
         (take 3)
         (map #(keyword (:name %))))))

;; ## Attributes

(defn apply-default-setters-from-attributes
  "Takes widget keywords name, UI widget object and attributes map
  after all custom attributes were applied. Transforms each attribute
  into a call to (.set_CapitalizedKey_ widget value). If value is a
  keyword then it is looked up in the keyword-mapping or if it is not
  there, it is perceived as a static field of the class."
  [widget-kw widget attributes]
  (doseq [[attribute value] attributes]
    (let [real-value (kw/value widget-kw value attribute)
          setter-name (keyword->setter attribute)]
      (let [method (try
                     (reflect-setter (type widget) setter-name (type real-value))
                     (catch NoSuchMethodException _
                       (let [suggestions (suggest-attributes widget-kw attribute)
                             msg (str attribute " is not a known attribute of "
                                      widget-kw
                                      (when (seq suggestions)
                                        (str "; did you mean "
                                             (string/join ", " suggestions)
                                             "?")))]
                         (throw (IllegalArgumentException. msg)))))]
        (.invoke method widget (into-array (vector real-value)))))))

(defn apply-attributes
  "Takes UI widget keyword, a widget object, a map of attributes and
  options. Consequently calls `apply-trait` on all element's traits,
  in the end calls `apply-default-setters-from-attributes` on what is
  left from the attributes map. Returns the updated options map.

  Options is a map of additional arguments that come from container
  elements to their inside elements. Note that all traits of the
  current element will receive the initial options map, and
  modifications will only appear visible to the subsequent elements."
  [widget-kw widget attributes options]
  (loop [[trait & rest] (kw/all-traits widget-kw),
         attrs attributes, new-opts options]
    (if trait
      (let [[attributes-fn options-fn]
            (try
              (apply-trait trait widget attrs options)
              (catch Exception e
                (let [;; Find which attributes this trait consumes
                      trait-attrs (get-in (meta #'apply-trait)
                                          [:attributes trait] [trait])
                      relevant (select-keys attrs trait-attrs)
                      msg (str "Error applying trait " trait " on " widget-kw
                               (when (seq relevant)
                                 (str " with " (pr-str relevant)))
                               ": " (.getMessage e))]
                  (throw (ex-info msg {::ui-error true
                                       :trait trait
                                       :widget widget-kw
                                       :values relevant}
                                  e)))))]
        (recur rest (attributes-fn attrs) (options-fn new-opts)))
      (do
        (apply-default-setters-from-attributes widget-kw widget attrs)
        new-opts))))

(declare config)

;; ## Widget creation

(defn construct-element
  "Constructs a UI widget by a given keyword. Infers a correct
  constructor for the types of arguments being passed to it."
  ([kw context constructor-args]
     (let [element-class (kw/classname kw)]
       (.newInstance (reflect-constructor element-class
                                          (cons android.content.Context
                                                (map type constructor-args)))
                     (to-array (cons context constructor-args))))))

(defn make-ui-element
  "Creates a UI widget based on its keyword name, applies attributes
  to it, then recursively create its subelements and add them to the
  widget. Reactive cells in attribute values are automatically detected
  and bound — the widget updates when the cell changes.

  The special `:on-create` attribute takes a function of one argument
  (the widget) and is called after all traits and children are applied.
  Use it for imperative initialization that depends on the fully
  constructed widget or its children."
  [context tree options]
  (if (sequential? tree)
    (let [[widget-kw attributes & inside-elements] tree
          _ (assert (and (keyword? widget-kw) (map? attributes))
                    (str "Invalid UI element: expected [keyword map ...], got "
                         (pr-str (take 2 tree))))]
      (try
        (let [attributes (merge (kw/default-attributes widget-kw) attributes)
              wdg (if-let [constr (:custom-constructor attributes)]
                    (apply constr context (:constructor-args attributes))
                    (construct-element widget-kw context
                                       (:constructor-args attributes)))
              init-fn (:on-create attributes)
              cleaned-attrs (dissoc attributes :constructor-args :custom-constructor :on-create)
              [resolved-attrs bindings] (extract-cells cleaned-attrs (cell-class))
              new-opts (apply-attributes widget-kw wdg resolved-attrs options)]
          (when (seq bindings)
            (let [wdg-weak (WeakReference. wdg)]
              (doseq [[attr c] bindings]
                (let [wkey [::reactive (System/identityHashCode wdg) attr]]
                  (add-watch c wkey
                    (fn [_ _ old-val new-val]
                      (if-let [w (.get wdg-weak)]
                        (when (not= old-val new-val)
                          (on-ui (config w attr new-val)))
                        (remove-watch c wkey))))))))
          (doseq [element inside-elements :when element]
            (try
              (.addView ^android.view.ViewGroup wdg
                        (make-ui-element context element new-opts))
              (catch Exception e
                (if (::ui-error (ex-data e))
                  (throw e)
                  (throw (ex-info (str "In child of " widget-kw ": "
                                       (.getMessage e))
                                  {::ui-error true
                                   :parent widget-kw
                                   :child-tree (if (sequential? element)
                                                 (take 2 element)
                                                 element)}
                                  e))))))
          (when init-fn (init-fn wdg))
          wdg)
        (catch clojure.lang.ExceptionInfo e
          (if (::ui-error (ex-data e))
            (throw e)
            (throw (ex-info (str "In " widget-kw ": " (.getMessage e))
                            {::ui-error true :widget widget-kw}
                            e))))
        (catch Exception e
          (throw (ex-info (str "In " widget-kw " " (pr-str (keys attributes)) ": "
                               (.getMessage e))
                          {::ui-error true
                           :widget widget-kw
                           :attributes (keys attributes)}
                          e)))))
    tree))

(defn make-ui
  "Takes an activity instance, and a tree of elements and creates Android UI
  elements according to this tree. A tree has a form of a vector that looks like
  following:

  `[element-name map-of-attributes & subelements]`."
  [activity tree]
  (make-ui-element activity tree {}))

(defn config
  "Takes a widget and key-value pairs of attributes, and applies these
  attributes to the widget."
  [widget & {:as attributes}]
  (apply-attributes (kw/keyword-by-classname (type widget))
                    widget attributes {}))

;; ## Compatibility with Android XML UI facilities.

(defn inflate-layout
  "Renders a View object for the given XML layout ID."
  [activity id]
  {:pre [(integer? id)]
   :post [(instance? android.view.View %)]}
  (.. android.view.LayoutInflater
      (from activity)
      (inflate ^Integer id nil)))

;; ## Utilities

(defn get-screen-orientation
  "Returns either :portrait, :landscape, or :undefined depending on the
  current orientation of the device."
  ([]
   (get-screen-orientation App/instance))
  ([context]
   (condp = (.. context (getResources) (getConfiguration) orientation)
     Configuration/ORIENTATION_PORTRAIT :portrait
     Configuration/ORIENTATION_LANDSCAPE :landscape
     :undefined)))
