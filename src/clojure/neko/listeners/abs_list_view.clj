(ns neko.listeners.abs-list-view
  "Utility functions and macros for creating listeners corresponding to the
  android.widget.AbsListView class."
  (:require [neko.debug :refer [safe-for-ui]]))

(defn on-scroll-call
  "Takes one or two functions and yields an AbsListView.OnScrollListener object
  that will invoke them.

  scroll-fn        called when the list has been scrolled.  Takes four arguments:
                   view               the AbsListView whose scroll state changed
                   first-visible-item  the index of the first visible cell
                   visible-item-count  the number of visible cells
                   total-item-count    the number of items in the list adapter

  scroll-state-fn  (optional) called when the scroll state changes.  Takes two
                   arguments:
                   view          the AbsListView whose scroll state changed
                   scroll-state  the current scroll state (int)"
  ^android.widget.AbsListView$OnScrollListener
  [scroll-fn & [scroll-state-fn]]
  (reify android.widget.AbsListView$OnScrollListener
    (onScroll [this view first-visible-item visible-item-count total-item-count]
      (safe-for-ui (scroll-fn view first-visible-item visible-item-count total-item-count)))
    (onScrollStateChanged [this view scroll-state]
      (safe-for-ui (when scroll-state-fn (scroll-state-fn view scroll-state))))))
