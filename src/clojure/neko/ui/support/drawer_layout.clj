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
  "Takes :on-drawer-opened (fn [drawer-view]), :on-drawer-closed (fn [drawer-view]),
  :on-drawer-slide (fn [drawer-view offset]), and/or :on-drawer-state-changed
  (fn [new-state]) attributes and adds a DrawerListener to the DrawerLayout."
  {:attributes [:on-drawer-opened :on-drawer-closed
                :on-drawer-slide :on-drawer-state-changed]}
  [^DrawerLayout wdg, {:keys [on-drawer-opened on-drawer-closed
                               on-drawer-slide on-drawer-state-changed]} _]
  (.addDrawerListener wdg (drawer-listeners/on-drawer-call
                           on-drawer-opened on-drawer-closed
                           on-drawer-slide on-drawer-state-changed)))
