(ns neko.ui.support.adapters
  "Contains a RecyclerView adapter helper, following the same ref-type pattern
  as `neko.ui.adapters/ref-adapter`."
  (:require [neko.debug :refer [safe-for-ui]]
            [neko.threading :refer [on-ui]]
            [neko.ui :refer [make-ui-element]]
            [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]])
  (:import android.view.View
           android.view.ViewGroup
           android.view.ViewGroup$LayoutParams
           android.widget.FrameLayout
           android.widget.FrameLayout$LayoutParams
           [neko.ui.adapters ClojureRecyclerAdapter]))

(defn recycler-adapter
  "Takes a function that creates a View for a RecyclerView item, a function
  that binds data to that View, and a reference type that stores the data.
  Returns a RecyclerView.Adapter that displays ref-type contents and
  automatically updates when the ref-type changes.

  `create-view-fn` is a function of two arguments: parent ViewGroup and
  view-type (int).  It should return a View (or a UI tree vector that will
  be inflated via `make-ui-element`).

  `bind-view-fn` is a function of three arguments: view, position (int),
  and the data element from the ref-type at that position.

  Optional keyword arguments:
  - `:access-fn`  called on the value of ref-type to get the list to be
                  displayed (default: `vec`).
  - `:item-id-fn` a function of two arguments (position, data-item) that
                  returns a long ID, enabling stable IDs."
  [create-view-fn bind-view-fn ref-type & {:keys [access-fn item-id-fn]
                                            :or {access-fn vec}}]
  {:pre [(fn? create-view-fn) (fn? bind-view-fn)
         (instance? clojure.lang.IDeref ref-type)]}
  (let [create-fn (fn [^ViewGroup parent view-type]
                    (or (safe-for-ui
                         (let [view (create-view-fn parent view-type)]
                           (if (instance? View view)
                             view
                             (make-ui-element
                              (.getContext parent) view {}))))
                        (View. (.getContext parent))))
        bind-fn (fn [view position data-item]
                  (safe-for-ui (bind-view-fn view position data-item)))
        adapter (ClojureRecyclerAdapter. create-fn bind-fn
                                         (access-fn @ref-type))]
    (when item-id-fn
      (.setItemIdFn adapter item-id-fn))
    (add-watch ref-type ::recycler-adapter-watch
               (fn [_ __ ___ new-state]
                 (on-ui (.setData adapter (access-fn new-state)))))
    adapter))

;; ## Declarative :items/:item-view trait for RecyclerView

(deftrait :recycler-items
  "Takes :items (an atom/ref of a collection) and :item-view (a function
  of two arguments: data-item and position, returning a UI tree vector).
  Creates a recycler-adapter internally and sets it on the RecyclerView.

  Example:

    [:recycler-view {:layout-manager :linear
                     :items my-data-atom
                     :item-view (fn [data pos]
                                  [:text-view {:text data
                                               :text-size [16 :sp]}])}]

  If :items is a plain collection (not an atom), it is wrapped in an atom
  but will not auto-update."
  {:attributes [:items :item-view]}
  [wdg, {:keys [items item-view]} _]
  (let [ref (if (instance? clojure.lang.IDeref items) items (atom items))
        adapter (recycler-adapter
                 ;; create-view-fn: a FrameLayout wrapper for re-inflation
                 (fn [^ViewGroup parent _view-type]
                   (let [fl (FrameLayout. (.getContext parent))]
                     (.setLayoutParams fl (FrameLayout$LayoutParams.
                                          ViewGroup$LayoutParams/MATCH_PARENT
                                          ViewGroup$LayoutParams/WRAP_CONTENT))
                     fl))
                 ;; bind-view-fn: clear wrapper, inflate item tree
                 (fn [view position data]
                   (let [^FrameLayout fl view
                         ctx (.getContext fl)]
                     (.removeAllViews fl)
                     (.addView fl (make-ui-element ctx (item-view data position) {}))))
                 ref)]
    (.setAdapter ^androidx.recyclerview.widget.RecyclerView wdg adapter)))

(kw/add-trait! :recycler-view :recycler-items)
