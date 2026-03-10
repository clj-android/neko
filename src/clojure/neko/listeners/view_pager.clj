(ns neko.listeners.view-pager
  "Utility functions for creating listeners corresponding to the
  androidx.viewpager.widget.ViewPager class."
  (:require [neko.debug :refer [safe-for-ui]]))

(defn on-page-change-call
  "Takes one to three functions and yields a ViewPager.OnPageChangeListener
  object that will invoke them.

  page-selected-fn  called when a new page becomes selected.  Takes one
                    argument: position (int).

  page-scrolled-fn  (optional) called when the current page is scrolled.
                    Takes three arguments: position (int), offset (float),
                    offset-pixels (int).

  scroll-state-fn   (optional) called when the scroll state changes.
                    Takes one argument: state (int)."
  ^androidx.viewpager.widget.ViewPager$OnPageChangeListener
  [page-selected-fn & [page-scrolled-fn scroll-state-fn]]
  (reify androidx.viewpager.widget.ViewPager$OnPageChangeListener
    (onPageSelected [this position]
      (safe-for-ui (when page-selected-fn (page-selected-fn position))))
    (onPageScrolled [this position offset offset-pixels]
      (safe-for-ui (when page-scrolled-fn
                     (page-scrolled-fn position offset offset-pixels))))
    (onPageScrollStateChanged [this state]
      (safe-for-ui (when scroll-state-fn (scroll-state-fn state))))))
