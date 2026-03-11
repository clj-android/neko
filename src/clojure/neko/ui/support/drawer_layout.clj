(ns neko.ui.support.drawer-layout
  "AndroidX DrawerLayout widget registration and traits.

  Requires `androidx.drawerlayout:drawerlayout` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait to-dimension
                                    apply-margins-to-layout-params]]
            [neko.listeners.drawer-layout :as drawer-listeners])
  (:import [androidx.drawerlayout.widget DrawerLayout DrawerLayout$LayoutParams]
           [android.view View]
           android.widget.TextView
           java.util.HashMap))

(kw/defelement :drawer-layout
  :classname DrawerLayout
  :inherits :view-group
  :traits [:on-drawer-opened :drawer-content])

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

;; ## Declarative navigation drawer

;; Tag key used to share the navigate! atom between :drawer-content and :nav-for.
(def ^:private nav-fn-key ::drawer-select!)

(deftrait :drawer-content
  "Declarative DrawerLayout navigation.

  Takes :drawer-content, a flat vector of alternating labels and content-view IDs:

    :drawer-content [\"Section 1\" ::section-1-id
                     \"Section 2\" ::section-2-id
                     ...]

  After the UI tree is built, finds the content views in the id-holder (which
  for a DrawerLayout with :id-holder true is the DrawerLayout itself), sets the
  first content view visible and the rest gone, then installs a navigate!
  function that:
    - shows the selected content view, hides the rest
    - closes the drawer
    - optionally updates a title view (via :drawer-title-id attribute)

  The navigate! function is stored in the DrawerLayout's tag map under the key
  neko.ui.support.drawer-layout/drawer-select! so that :nav-for traits on
  child views can call it.

  Combine with :nav-for on drawer-panel nav items and :opens-drawer on the
  hamburger button for a fully declarative drawer navigation setup.

  Example:

    [:drawer-layout {:id-holder true
                     :drawer-content [\"Home\"     ::home-panel
                                      \"Settings\" ::settings-panel]
                     :drawer-title-id ::toolbar-title}
     [:linear-layout {}   ; content area
      [:button {:opens-drawer true :text \"☰\"}]
      [:text-view {:id ::toolbar-title :text \"Home\"}]
      [:frame-layout {}
       [:text-view {:id ::home-panel     :text \"Home content\"}]
       [:text-view {:id ::settings-panel :text \"Settings content\" :visibility :gone}]]]
     [:linear-layout {:layout-gravity :start}   ; drawer panel
      [:text-view {:text \"Home\"     :nav-for ::home-panel}]
      [:text-view {:text \"Settings\" :nav-for ::settings-panel}]]]"
  {:attributes [:drawer-content :drawer-title-id]}
  [^DrawerLayout wdg, {:keys [drawer-content drawer-title-id]} _]
  ;; Note: :drawer-content runs before :id-holder (own traits precede inherited
  ;; ones in all-traits order). So (.getTag wdg) is null at trait time.
  ;; Defer ALL work to .post — by then :id-holder has set the tag and child
  ;; :id traits have populated it with content-view references.
  (let [pairs       (partition 2 drawer-content)
        labels      (mapv first pairs)
        ids         (mapv second pairs)
        select-atom (atom nil)]
    (.post wdg
      (fn []
        (let [tag ^HashMap (.getTag wdg)]
          (when tag
            ;; Store the atom so :nav-for click handlers can find it at runtime.
            (.put tag nav-fn-key select-atom)
            (let [views (mapv #(.get tag %) ids)]
              ;; Set initial visibility: first section shown, rest hidden.
              (dotimes [i (count views)]
                (when-let [^View v (nth views i)]
                  (.setVisibility v (if (zero? i) View/VISIBLE View/GONE))))
              ;; Install the navigate! function.
              (reset! select-atom
                (fn [section-id]
                  (let [idx (first (keep-indexed
                                     (fn [i id] (when (= id section-id) i))
                                     ids))]
                    (when idx
                      (dotimes [i (count views)]
                        (when-let [^View v (nth views i)]
                          (.setVisibility v (if (= i idx) View/VISIBLE View/GONE))))
                      (when drawer-title-id
                        (when-let [^TextView tv (.get tag drawer-title-id)]
                          (.setText tv ^CharSequence (nth labels idx ""))))
                      (.closeDrawers wdg))))))))))))



(deftrait :nav-for
  "Wires a view inside the drawer panel to a content section managed by
  :drawer-content on the enclosing DrawerLayout.

  Takes :nav-for, a keyword matching one of the content-view IDs declared in
  :drawer-content, and installs an OnClickListener that calls the navigate!
  function with that section ID.

  The navigate! atom is looked up from the DrawerLayout's tag at click time
  (not at construction time), so there is no ordering dependency with :drawer-content.

  The enclosing DrawerLayout must have :id-holder true and :drawer-content.

  Example:

    [:text-view {:text \"Settings\" :nav-for ::settings-panel}]"
  [^View wdg, {:keys [nav-for]} {:keys [^View id-holder]}]
  ;; Capture id-holder (the DrawerLayout) at construction time.
  ;; Look up the navigate fn at click time — by then .post has stored the atom.
  (.setOnClickListener
    wdg
    (reify android.view.View$OnClickListener
      (onClick [_ _]
        (when id-holder
          (let [tag ^HashMap (.getTag id-holder)
                a   (when tag (.get tag nav-fn-key))]
            (when-let [f (and a @a)]
              (f nav-for))))))))

(deftrait :opens-drawer
  "Installs an OnClickListener on the widget that opens the enclosing DrawerLayout.

  Takes :opens-drawer true. The enclosing DrawerLayout must have :id-holder true.

  Example:

    [:button {:text \"☰\" :opens-drawer true}]"
  [^View wdg, {:keys [opens-drawer]} {:keys [^View id-holder]}]
  (when (and opens-drawer (instance? DrawerLayout id-holder))
    (.setOnClickListener
      wdg
      (reify android.view.View$OnClickListener
        (onClick [_ _]
          (.openDrawer ^DrawerLayout id-holder
                       (int android.view.Gravity/START)))))))

;; Register :nav-for and :opens-drawer on :view so all widgets pick them up.
(kw/add-trait! :view :nav-for)
(kw/add-trait! :view :opens-drawer)
