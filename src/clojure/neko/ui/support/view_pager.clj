(ns neko.ui.support.view-pager
  "AndroidX ViewPager widget registration and traits.

  Requires `androidx.viewpager:viewpager` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]]
            [neko.listeners.view-pager :as pager-listeners])
  (:import [androidx.viewpager.widget ViewPager]))

(kw/defelement :view-pager
  :classname ViewPager
  :inherits :view-group
  :traits [:on-page-change])

(deftrait :on-page-change
  "Takes :on-page-change (fn [position]), :on-page-scrolled
  (fn [position offset offset-pixels]), and/or :on-page-scroll-state-changed
  (fn [state]) attributes and adds an OnPageChangeListener to the ViewPager."
  {:attributes [:on-page-change :on-page-scrolled :on-page-scroll-state-changed]}
  [^ViewPager wdg, {:keys [on-page-change on-page-scrolled
                            on-page-scroll-state-changed]} _]
  (.addOnPageChangeListener wdg (pager-listeners/on-page-change-call
                                 on-page-change on-page-scrolled
                                 on-page-scroll-state-changed)))
