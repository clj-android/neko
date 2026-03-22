(ns neko.ui.adapters
  "Contains custom adapters for ListView and Spinner."
  (:require [neko.debug :refer [safe-for-ui]]
            [neko.data.sqlite :refer [entity-from-cursor]]
            [neko.threading :refer [on-ui]]
            [neko.ui :refer [make-ui-element]]
            [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]])
  (:import android.view.View
           [android.view ViewGroup ViewGroup$LayoutParams]
           android.widget.FrameLayout
           android.widget.FrameLayout$LayoutParams
           neko.data.sqlite.TaggedCursor
           [neko.ui.adapters InterchangeableListAdapter TaggedCursorAdapter]))

(defn ref-adapter
  "Takes a function that creates a View, a function that updates a
  view according to the element and a reference type that stores the
  data. Returns an Adapter object that displays ref-type contents.
  When ref-type is updated, Adapter gets updated as well.

  `create-view-fn` is a function of context. `update-view-fn` is
  a function of four arguments: element position, view to update,
  parent view container and the respective data element from the
  ref-type. `access-fn` argument is optional, it is called on the
  value of ref-type to get the list to be displayed."
  ([create-view-fn update-view-fn ref-type]
   (ref-adapter create-view-fn update-view-fn ref-type identity))
  ([create-view-fn update-view-fn ref-type access-fn]
   {:pre [(fn? create-view-fn) (fn? update-view-fn)
          (instance? clojure.lang.IFn access-fn)
          (instance? clojure.lang.IDeref ref-type)]}
   (let [create-fn (fn [context]
                     (or (safe-for-ui
                          (let [view (create-view-fn context)]
                            (if (instance? View view)
                              view
                              (make-ui-element
                               context view
                               {:container-type :abs-listview-layout}))))
                         (android.view.View. context)))
         adapter (InterchangeableListAdapter.
                  create-fn
                  (fn [pos view parent data]
                    (safe-for-ui (update-view-fn pos view parent data)))
                  (access-fn @ref-type))]
     (add-watch ref-type ::adapter-watch
                (fn [_ __ ___ new-state]
                  (on-ui (.setData adapter (access-fn new-state)))))
     adapter)))

(defn cursor-adapter
  "Takes a context, a function that creates a View, and a function that updates
  a view according to the element, and a TaggedCursor instance or
  cursor-producing function. Returns an Adapter object that displays cursor
  contents.

  `create-view-fn` is a nullary function that returns a UI tree or a View.
  `update-view-fn` is a function of three arguments: view to update, cursor, and
  data extracted from the cursor. `cursor-or-cursor-fn` can be a nullary
  function that returns a TaggedCursor cursor object when called, or just a
  cursor. In the former case you can refresh adapter by calling `(.updateCursor
  adapter)`, in the latter you have to call `(.updateCursor adapter
  new-cursor)`."
  [context create-view-fn update-view-fn cursor-or-cursor-fn]
  {:pre [(fn? create-view-fn) (fn? update-view-fn)
         (or (fn? cursor-or-cursor-fn)
             (instance? TaggedCursor cursor-or-cursor-fn))]}
  (let [create-fn (fn [context]
                    (or (safe-for-ui
                         (let [view (create-view-fn)]
                           (if (instance? View view)
                             view
                             (make-ui-element
                              context view
                              {:container-type :abs-listview-layout}))))
                        (android.view.View. context)))]
    (TaggedCursorAdapter.
     context create-fn
     (fn [view cursor data]
       (safe-for-ui (update-view-fn view cursor data)))
     cursor-or-cursor-fn)))

(defn update-cursor
  "Updates cursor in a given TaggedCursorAdapter. Second argument is necessary
  if the adapter was created with a cursor rather than cursor-fn."
  ([^TaggedCursorAdapter cursor-adapter]
   (.updateCursor cursor-adapter))
  ([^TaggedCursorAdapter cursor-adapter new-cursor]
   (.updateCursor cursor-adapter new-cursor)))

;; ## Declarative :items/:item-view trait for ListView

(deftrait :list-items
  "Takes :items (an atom/ref of a collection) and :item-view (a function
  of two arguments: data-item and position, returning a UI tree vector).
  Creates a ref-adapter internally and sets it on the ListView.

  Example:

    [:list-view {:items my-data-atom
                 :item-view (fn [data pos]
                              [:text-view {:text (:name data)
                                           :text-size [16 :sp]}])}]

  If :items is a plain collection (not an atom), it is wrapped in an atom
  but will not auto-update."
  {:attributes [:items :item-view]}
  [wdg, {:keys [items item-view]} _]
  (let [ref (if (instance? clojure.lang.IDeref items) items (atom items))
        adapter (ref-adapter
                 ;; create-view-fn: a FrameLayout wrapper for re-inflation
                 (fn [context]
                   (let [fl (FrameLayout. context)]
                     (.setLayoutParams fl (FrameLayout$LayoutParams.
                                          ViewGroup$LayoutParams/MATCH_PARENT
                                          ViewGroup$LayoutParams/WRAP_CONTENT))
                     fl))
                 ;; update-view-fn: clear wrapper, inflate item tree
                 (fn [pos view _parent data]
                   (let [^FrameLayout fl view
                         ctx (.getContext fl)]
                     (.removeAllViews fl)
                     (.addView fl (make-ui-element ctx (item-view data pos) {}))))
                 ref)]
    (.setAdapter ^android.widget.ListView wdg adapter)))

(kw/add-trait! :list-view :list-items)
