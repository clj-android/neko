(ns neko.ui.support.toolbar
  "AndroidX Toolbar widget registration and traits.

  Requires `androidx.appcompat:appcompat` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]]
            [neko.listeners.view :as view-listeners]
            [neko.resource :as res])
  (:import [androidx.appcompat.widget Toolbar]))

(kw/defelement :toolbar
  :classname Toolbar
  :inherits :view-group
  :traits [:toolbar-title :toolbar-subtitle :navigation-icon :on-navigation-click])

(deftrait :toolbar-title
  "Takes :toolbar-title attribute (string or string resource) and sets the
  Toolbar's title."
  [^Toolbar wdg, {:keys [toolbar-title]} _]
  (.setTitle wdg ^CharSequence (res/get-string (.getContext wdg) toolbar-title)))

(deftrait :toolbar-subtitle
  "Takes :toolbar-subtitle attribute (string or string resource) and sets the
  Toolbar's subtitle."
  [^Toolbar wdg, {:keys [toolbar-subtitle]} _]
  (.setSubtitle wdg ^CharSequence (res/get-string (.getContext wdg) toolbar-subtitle)))

(deftrait :navigation-icon
  "Takes :navigation-icon attribute (drawable resource ID) and sets the
  Toolbar's navigation icon."
  [^Toolbar wdg, {:keys [navigation-icon]} _]
  (.setNavigationIcon wdg (int navigation-icon)))

(deftrait :on-navigation-click
  "Takes :on-navigation-click attribute, which should be a function of one
  argument (view), and sets it as the Toolbar's navigation OnClickListener."
  [^Toolbar wdg, {:keys [on-navigation-click]} _]
  (.setNavigationOnClickListener wdg (view-listeners/on-click-call on-navigation-click)))
