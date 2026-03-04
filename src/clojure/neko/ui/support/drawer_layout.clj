(ns neko.ui.support.drawer-layout
  "AndroidX DrawerLayout widget registration and traits.

  Requires `androidx.drawerlayout:drawerlayout` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]]
            [neko.listeners.drawer-layout :as drawer-listeners])
  (:import [androidx.drawerlayout.widget DrawerLayout]))

(kw/defelement :drawer-layout
  :classname DrawerLayout
  :inherits :view-group
  :traits [:on-drawer-opened])

(deftrait :on-drawer-opened
  "Takes :on-drawer-opened attribute, which should be a function of one
  argument (drawer-view), and adds it as the DrawerLayout's DrawerListener."
  [^DrawerLayout wdg, {:keys [on-drawer-opened]} _]
  (.addDrawerListener wdg (drawer-listeners/on-drawer-call on-drawer-opened)))
