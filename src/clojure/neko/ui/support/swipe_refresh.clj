(ns neko.ui.support.swipe-refresh
  "AndroidX SwipeRefreshLayout widget registration and traits.

  Requires `androidx.swiperefreshlayout:swiperefreshlayout` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]]
            [neko.listeners.swipe-refresh :as swipe-listeners])
  (:import [androidx.swiperefreshlayout.widget SwipeRefreshLayout]))

(kw/defelement :swipe-refresh-layout
  :classname SwipeRefreshLayout
  :inherits :view-group
  :traits [:on-refresh :refreshing])

(deftrait :on-refresh
  "Takes :on-refresh attribute, which should be a zero-argument function, and
  sets it as the SwipeRefreshLayout's OnRefreshListener."
  [^SwipeRefreshLayout wdg, {:keys [on-refresh]} _]
  (.setOnRefreshListener wdg (swipe-listeners/on-refresh-call on-refresh)))

(deftrait :refreshing
  "Takes :refreshing attribute (boolean) and sets whether the
  SwipeRefreshLayout's refresh indicator is shown."
  [^SwipeRefreshLayout wdg, {:keys [refreshing]} _]
  (.setRefreshing wdg (boolean refreshing)))
