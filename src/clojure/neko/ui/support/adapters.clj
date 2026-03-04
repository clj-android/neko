(ns neko.ui.support.adapters
  "Contains a RecyclerView adapter helper, following the same ref-type pattern
  as `neko.ui.adapters/ref-adapter`."
  (:require [neko.debug :refer [safe-for-ui]]
            [neko.threading :refer [on-ui]]
            [neko.ui :refer [make-ui-element]])
  (:import android.view.View
           android.view.ViewGroup
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
