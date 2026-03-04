(ns neko.ui.support.coordinator-layout
  "AndroidX CoordinatorLayout and NestedScrollView widget registrations,
  plus the :coordinator-layout-params trait.

  Requires `androidx.coordinatorlayout:coordinatorlayout` and
  `androidx.core:core` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait to-dimension apply-margins-to-layout-params]]
            [neko.internal])
  (:import [androidx.coordinatorlayout.widget CoordinatorLayout
            CoordinatorLayout$LayoutParams]
           [androidx.core.widget NestedScrollView]
           [android.view View]))

(kw/defelement :coordinator-layout
  :classname CoordinatorLayout
  :inherits :view-group)

(kw/defelement :nested-scroll-view
  :classname NestedScrollView
  :inherits :frame-layout)

;; CoordinatorLayout layout params trait

(def ^:private coordinator-layout-margin-attributes
  [:layout-margin :layout-margin-left :layout-margin-top
   :layout-margin-right :layout-margin-bottom])

(deftrait :coordinator-layout-params
  "Takes :layout-width, :layout-height, :layout-gravity, layout margin
  attributes, and :layout-anchor / :layout-anchor-gravity for
  CoordinatorLayout.LayoutParams."
  {:attributes (concat coordinator-layout-margin-attributes
                       [:layout-width :layout-height :layout-gravity
                        :layout-anchor :layout-anchor-gravity])
   :applies? (= container-type :coordinator-layout)}
  [^View wdg, {:keys [layout-width layout-height layout-gravity
                       layout-anchor layout-anchor-gravity]
                :as attributes}
   {:keys [container-type]}]
  (let [^int width (->> (or layout-width :wrap)
                        (kw/value :layout-params)
                        (to-dimension (.getContext wdg)))
        ^int height (->> (or layout-height :wrap)
                         (kw/value :layout-params)
                         (to-dimension (.getContext wdg)))
        params (CoordinatorLayout$LayoutParams. width height)]
    (apply-margins-to-layout-params (.getContext wdg) params attributes)
    (when layout-gravity
      (set! (. params gravity)
            (kw/value :layout-params layout-gravity :gravity)))
    (when layout-anchor
      (set! (. params anchorId)
            (neko.internal/int-id layout-anchor)))
    (when layout-anchor-gravity
      (set! (. params anchorGravity)
            (kw/value :layout-params layout-anchor-gravity :gravity)))
    (.setLayoutParams wdg params)))

;; Register coordinator-layout-params on :view so children of
;; CoordinatorLayout pick it up automatically.
(kw/add-trait! :view :coordinator-layout-params)
