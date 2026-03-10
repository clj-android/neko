(ns neko.listeners.drawer-layout
  "Utility functions for creating listeners corresponding to the
  androidx.drawerlayout.widget.DrawerLayout class."
  (:require [neko.debug :refer [safe-for-ui]]))

(defn on-drawer-call
  "Takes one to four functions and yields a DrawerLayout.DrawerListener object
  that will invoke them.

  opened-fn        called when a drawer has settled in a fully open state.
                   Takes one argument: drawer-view.

  closed-fn        (optional) called when a drawer has settled in a fully
                   closed state.  Takes one argument: drawer-view.

  slide-fn         (optional) called as the drawer's position changes.
                   Takes two arguments: drawer-view, slide-offset (float).

  state-changed-fn (optional) called when the drawer motion state changes.
                   Takes one argument: new-state (int)."
  ^androidx.drawerlayout.widget.DrawerLayout$DrawerListener
  [opened-fn & [closed-fn slide-fn state-changed-fn]]
  (reify androidx.drawerlayout.widget.DrawerLayout$DrawerListener
    (onDrawerOpened [this drawer-view]
      (safe-for-ui (when opened-fn (opened-fn drawer-view))))
    (onDrawerClosed [this drawer-view]
      (safe-for-ui (when closed-fn (closed-fn drawer-view))))
    (onDrawerSlide [this drawer-view slide-offset]
      (safe-for-ui (when slide-fn (slide-fn drawer-view slide-offset))))
    (onDrawerStateChanged [this new-state]
      (safe-for-ui (when state-changed-fn (state-changed-fn new-state))))))
