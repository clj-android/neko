(ns neko.listeners.swipe-refresh
  "Utility functions for creating listeners corresponding to the
  androidx.swiperefreshlayout.widget.SwipeRefreshLayout class."
  (:require [neko.debug :refer [safe-for-ui]]))

(defn on-refresh-call
  "Takes a function and yields a SwipeRefreshLayout.OnRefreshListener object
  that will invoke the function.  This function takes no arguments and is called
  when the user triggers a refresh gesture."
  ^androidx.swiperefreshlayout.widget.SwipeRefreshLayout$OnRefreshListener
  [handler-fn]
  (reify androidx.swiperefreshlayout.widget.SwipeRefreshLayout$OnRefreshListener
    (onRefresh [this]
      (safe-for-ui (handler-fn)))))
