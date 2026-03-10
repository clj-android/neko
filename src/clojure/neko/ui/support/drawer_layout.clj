(ns neko.ui.support.drawer-layout
  "AndroidX DrawerLayout widget registration and traits.

  Requires `androidx.drawerlayout:drawerlayout` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait to-dimension
                                    apply-margins-to-layout-params]]
            [neko.listeners.drawer-layout :as drawer-listeners])
  (:import [androidx.drawerlayout.widget DrawerLayout DrawerLayout$LayoutParams]
           [android.view View]))

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

;; ## DrawerLayout LayoutParams

(def ^:private drawer-margin-attributes
  [:layout-margin :layout-margin-left :layout-margin-top
   :layout-margin-right :layout-margin-bottom])

(deftrait :drawer-layout-params
  "Takes :layout-width, :layout-height, :layout-gravity, and layout margin
  attributes and sets DrawerLayout.LayoutParams on children of DrawerLayout.

  Use :layout-gravity :start on the drawer panel child so DrawerLayout
  recognises it as the sliding drawer."
  {:attributes (concat drawer-margin-attributes
                       [:layout-width :layout-height :layout-gravity])
   :applies? (= container-type :drawer-layout)}
  [^View wdg, {:keys [layout-width layout-height layout-gravity]
                :as attributes}
   {:keys [container-type]}]
  (let [^int width (->> (or layout-width :wrap)
                        (kw/value :layout-params)
                        (to-dimension (.getContext wdg)))
        ^int height (->> (or layout-height :wrap)
                         (kw/value :layout-params)
                         (to-dimension (.getContext wdg)))
        params (DrawerLayout$LayoutParams. width height)]
    (apply-margins-to-layout-params (.getContext wdg) params attributes)
    (when layout-gravity
      (set! (. params gravity)
            (kw/value :layout-params layout-gravity :gravity)))
    (.setLayoutParams wdg params)))

;; Register on :view so all children of DrawerLayout pick it up.
(kw/add-trait! :view :drawer-layout-params)
