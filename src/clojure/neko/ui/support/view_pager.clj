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
  "Takes :on-page-change attribute, which should be a function of one argument
  (position), and adds it as the ViewPager's OnPageChangeListener."
  [^ViewPager wdg, {:keys [on-page-change]} _]
  (.addOnPageChangeListener wdg (pager-listeners/on-page-change-call on-page-change)))
