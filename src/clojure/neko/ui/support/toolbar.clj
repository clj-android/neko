(ns neko.ui.support.toolbar
  "AndroidX Toolbar widget registration and traits.

  Requires `androidx.appcompat:appcompat` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]]
            [neko.listeners.view :as view-listeners]
            [neko.resource :as res]
            [neko.internal :refer [int-id]])
  (:import [androidx.appcompat.widget Toolbar]
           [android.view Menu MenuItem]))

(kw/defelement :toolbar
  :classname Toolbar
  :inherits :view-group
  :traits [:toolbar-title :toolbar-subtitle :navigation-icon
           :on-navigation-click :menu])

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

(deftrait :menu
  "Takes :menu attribute, a vector of menu item maps, and optionally
  :on-menu-item-click, a function of one argument (the keyword :id of the
  clicked item, or the integer ID if no keyword was mapped).

  Each map in :menu supports:
    :id              keyword ID for the menu item
    :title           string title (required)
    :icon            integer drawable resource ID
    :show-as-action  :always, :if-room, or :never (default :never)

  Example:

    [:toolbar {:toolbar-title \"My App\"
               :menu [{:id ::search :title \"Search\"
                        :show-as-action :always}
                       {:id ::settings :title \"Settings\"}]
               :on-menu-item-click (fn [id] (handle-menu id))}]"
  {:attributes [:menu :on-menu-item-click]}
  [^Toolbar wdg, {:keys [menu on-menu-item-click]} _]
  (let [^Menu m (.getMenu wdg)
        id-reverse (java.util.HashMap.)]
    (doseq [{:keys [id title icon show-as-action]} menu]
      (let [item-id (if id (int-id id) Menu/NONE)
            ^MenuItem item (.add m (int Menu/NONE) (int item-id)
                                (int Menu/NONE) ^CharSequence (str title))]
        (when id (.put id-reverse (int item-id) id))
        (when icon (.setIcon item (int icon)))
        (when show-as-action
          (.setShowAsAction item
            (int (case show-as-action
                   :always  MenuItem/SHOW_AS_ACTION_ALWAYS
                   :if-room MenuItem/SHOW_AS_ACTION_IF_ROOM
                   :never   MenuItem/SHOW_AS_ACTION_NEVER))))))
    (when on-menu-item-click
      (.setOnMenuItemClickListener wdg
        (reify androidx.appcompat.widget.Toolbar$OnMenuItemClickListener
          (onMenuItemClick [_ item]
            (let [iid (.getItemId item)]
              (on-menu-item-click (or (.get id-reverse iid) iid)))
            true))))))
