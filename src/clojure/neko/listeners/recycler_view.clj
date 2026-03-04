(ns neko.listeners.recycler-view
  "Utility functions for creating listeners corresponding to the
  androidx.recyclerview.widget.RecyclerView class."
  (:require [neko.debug :refer [safe-for-ui]]))

(defn on-scroll-call
  "Takes one or two functions and yields a RecyclerView.OnScrollListener object
  that will invoke them.

  scroll-fn       called when the RecyclerView has been scrolled.  Takes three
                  arguments: recycler-view, dx (int), dy (int).

  scroll-state-fn (optional) called when the scroll state changes.  Takes two
                  arguments: recycler-view, new-state (int)."
  [scroll-fn & [scroll-state-fn]]
  (proxy [androidx.recyclerview.widget.RecyclerView$OnScrollListener] []
    (onScrolled [recycler-view dx dy]
      (safe-for-ui (scroll-fn recycler-view dx dy)))
    (onScrollStateChanged [recycler-view new-state]
      (safe-for-ui (when scroll-state-fn (scroll-state-fn recycler-view new-state))))))
