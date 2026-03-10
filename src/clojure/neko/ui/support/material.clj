(ns neko.ui.support.material
  "AndroidX Material Components widget registrations: TabLayout,
  FloatingActionButton, and AppBarLayout.

  Requires `com.google.android.material:material` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]]
            [neko.listeners.tab-layout :as tab-listeners]
            [neko.listeners.view :as view-listeners])
  (:import [com.google.android.material.appbar AppBarLayout]
           [com.google.android.material.floatingactionbutton FloatingActionButton]
           [com.google.android.material.tabs TabLayout TabLayout$Tab]
           [android.view View]
           [java.util HashMap]))

;; Widget registrations

(kw/defelement :tab-layout
  :classname TabLayout
  :inherits :view-group
  :traits [:on-tab-selected :tab-mode :tab-gravity :tabs :tab-content])

(kw/defelement :floating-action-button
  :classname FloatingActionButton
  :inherits :image-view
  :traits [:fab-size])

(kw/defelement :app-bar-layout
  :classname AppBarLayout
  :inherits :linear-layout)

;; TabLayout traits

(deftrait :on-tab-selected
  "Takes :on-tab-selected attribute, which should be a function of one
  argument (tab), and adds it as the TabLayout's OnTabSelectedListener."
  [^TabLayout wdg, {:keys [on-tab-selected]} _]
  (.addOnTabSelectedListener wdg (tab-listeners/on-tab-selected-call on-tab-selected)))

(deftrait :tab-mode
  "Takes :tab-mode attribute, which should be :fixed or :scrollable,
  and sets the TabLayout's tab mode."
  [^TabLayout wdg, {:keys [tab-mode]} _]
  (.setTabMode wdg (int (case tab-mode
                          :fixed     TabLayout/MODE_FIXED
                          :scrollable TabLayout/MODE_SCROLLABLE))))

(deftrait :tab-gravity
  "Takes :tab-gravity attribute, which should be :fill or :center,
  and sets the TabLayout's tab gravity."
  [^TabLayout wdg, {:keys [tab-gravity]} _]
  (.setTabGravity wdg (int (case tab-gravity
                             :fill   TabLayout/GRAVITY_FILL
                             :center TabLayout/GRAVITY_CENTER))))

(deftrait :tabs
  "Takes :tabs attribute, a vector of tab specifications. Each entry can be:
  - a string (shorthand for {:text \"label\"})
  - a map with keys :text, :icon, :content-description
  Creates and adds tabs to the TabLayout in order."
  [^TabLayout wdg, {:keys [tabs]} _]
  (doseq [tab-spec tabs]
    (let [tab (.newTab wdg)
          spec (if (string? tab-spec) {:text tab-spec} tab-spec)]
      (when-let [t (:text spec)] (.setText tab ^CharSequence t))
      (when-let [i (:icon spec)]
        (if (integer? i)
          (.setIcon tab (int i))
          (.setIcon tab i)))
      (when-let [cd (:content-description spec)]
        (.setContentDescription tab ^CharSequence cd))
      (.addTab wdg tab))))

(deftrait :tab-content
  "Takes :tab-content attribute, a flat vector of [label id label id ...]
  pairs mapping tab labels to sibling content view IDs. Creates tabs and
  auto-wires visibility switching: the selected tab's content view is
  shown (VISIBLE), all others are hidden (GONE).

  Content views are resolved from the nearest :id-holder ancestor after
  the full UI tree is built. Do not combine with :tabs (use one or the
  other). Can be combined with :on-tab-selected for additional behavior.

  Example:

    [:tab-layout {:tab-content [\"Widgets\" ::widgets-panel
                                \"REPL\"    ::repl-panel]
                  :tab-mode :fixed}]"
  [^TabLayout wdg, {:keys [tab-content]} {:keys [^View id-holder]}]
  (let [pairs (partition 2 tab-content)]
    ;; Create the tabs
    (doseq [[label _] pairs]
      (.addTab wdg (doto (.newTab wdg) (.setText ^CharSequence (str label)))))
    ;; Defer view resolution until the full UI tree is built.
    ;; During make-ui, sibling views don't exist yet when this trait runs.
    (.post wdg
      (fn []
        (when id-holder
          (let [tag ^HashMap (.getTag id-holder)
                views (mapv (fn [[_ id]] (.get tag id)) pairs)]
            ;; Set initial visibility: first panel shown, rest hidden
            (dotimes [i (count views)]
              (when-let [^View v (nth views i)]
                (.setVisibility v (if (zero? i) View/VISIBLE View/GONE))))
            ;; Wire up tab switching
            (.addOnTabSelectedListener wdg
              (tab-listeners/on-tab-selected-call
                (fn [tab]
                  (let [pos (.getPosition ^TabLayout$Tab tab)]
                    (dotimes [i (count views)]
                      (when-let [^View v (nth views i)]
                        (.setVisibility v (if (= i pos)
                                            View/VISIBLE
                                            View/GONE))))))))))))))

;; FloatingActionButton traits

(deftrait :fab-size
  "Takes :fab-size attribute, which should be :normal, :mini, or :auto,
  and sets the FAB's size."
  [^FloatingActionButton wdg, {:keys [fab-size]} _]
  (.setSize wdg (int (case fab-size
                       :normal FloatingActionButton/SIZE_NORMAL
                       :mini   FloatingActionButton/SIZE_MINI
                       :auto   FloatingActionButton/SIZE_AUTO))))
