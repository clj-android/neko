(ns neko.listeners.tab-layout
  "Utility functions for creating listeners corresponding to the
  com.google.android.material.tabs.TabLayout class."
  (:require [neko.debug :refer [safe-for-ui]]))

(defn on-tab-selected-call
  "Takes one to three functions and yields a TabLayout.OnTabSelectedListener
  object that will invoke them.

  selected-fn     called when a tab enters the selected state.
                  Takes one argument: tab.

  unselected-fn   (optional) called when a tab exits the selected state.
                  Takes one argument: tab.

  reselected-fn   (optional) called when a tab that is already selected is
                  chosen again by the user.  Takes one argument: tab."
  [selected-fn & [unselected-fn reselected-fn]]
  (reify com.google.android.material.tabs.TabLayout$OnTabSelectedListener
    (onTabSelected [this tab]
      (safe-for-ui (selected-fn tab)))
    (onTabUnselected [this tab]
      (safe-for-ui (when unselected-fn (unselected-fn tab))))
    (onTabReselected [this tab]
      (safe-for-ui (when reselected-fn (reselected-fn tab))))))
