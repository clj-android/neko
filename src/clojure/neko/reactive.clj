(ns neko.reactive
  "Minimal reactive cell library for Neko.

  Cells are reactive values with automatic dependency tracking:
    (cell val)        - input cell, supports swap!/reset!/@
    (cell= #(expr))   - formula cell, auto-recomputes when deps change
    (bind! w :attr c)  - bind widget attribute to cell"
  (:require [neko.ui :refer [config]]
            [neko.threading :refer [on-ui]])
  (:import [java.lang.ref WeakReference]))

(def ^:dynamic *tracking*
  "When bound to an atom containing a set, deref'ing a Cell adds it
  to the set. Used by cell= for automatic dependency tracking."
  nil)

(deftype Cell [state]
  clojure.lang.IDeref
  (deref [this]
    (when *tracking* (swap! *tracking* conj this))
    @state)

  clojure.lang.IRef
  (addWatch [this key f]
    (add-watch state key (fn [k _ o n] (f k this o n)))
    this)
  (removeWatch [this key]
    (remove-watch state key)
    this)
  (getWatches [_]
    (.getWatches ^clojure.lang.IRef state))
  (getValidator [_] nil)
  (setValidator [_ _])

  clojure.lang.IAtom
  (swap [_ f] (.swap ^clojure.lang.IAtom state f))
  (swap [_ f arg] (.swap ^clojure.lang.IAtom state f arg))
  (swap [_ f arg1 arg2] (.swap ^clojure.lang.IAtom state f arg1 arg2))
  (swap [_ f x y args] (.swap ^clojure.lang.IAtom state f x y args))
  (reset [_ v] (.reset ^clojure.lang.IAtom state v))
  (compareAndSet [_ o n] (.compareAndSet ^clojure.lang.IAtom state o n)))

(defn cell?
  "Returns true if v is a reactive Cell."
  [v]
  (instance? Cell v))

(defn cell
  "Creates an input cell with the given initial value.
  Supports swap!, reset!, deref, add-watch, remove-watch."
  [initial-value]
  (Cell. (atom initial-value)))

(defn cell=
  "Creates a formula cell. f is a zero-arg function whose body derefs
  other cells. The cell auto-recomputes when any dependency changes."
  [f]
  (let [result (cell nil)
        deps (atom #{})]
    (letfn [(recompute []
              (let [tracking (atom #{})
                    new-val (binding [*tracking* tracking] (f))
                    new-deps @tracking
                    old-deps @deps]
                (doseq [d old-deps :when (not (new-deps d))]
                  (.removeWatch ^clojure.lang.IRef d [::formula result]))
                (doseq [d new-deps :when (not (old-deps d))]
                  (.addWatch ^clojure.lang.IRef d [::formula result]
                    (fn [_ _ _ _] (recompute))))
                (reset! deps new-deps)
                (reset! result new-val)))]
      (recompute)
      result)))

(defn bind!
  "Binds a widget attribute to a cell. When the cell changes,
  the widget is updated on the UI thread via neko.ui/config.
  Uses a WeakReference to avoid preventing widget GC."
  [widget attr cell]
  (on-ui (config widget attr @cell))
  (let [weak-ref (WeakReference. widget)
        watch-key [::bind (System/identityHashCode widget) attr]]
    (.addWatch ^clojure.lang.IRef cell watch-key
      (fn [_ _ old-val new-val]
        (if-let [w (.get weak-ref)]
          (when (not= old-val new-val)
            (on-ui (config w attr new-val)))
          (.removeWatch ^clojure.lang.IRef cell watch-key)))))
  cell)

(defn unbind!
  "Removes a reactive binding between a widget attribute and a cell."
  [widget attr cell]
  (.removeWatch ^clojure.lang.IRef cell
    [::bind (System/identityHashCode widget) attr]))
