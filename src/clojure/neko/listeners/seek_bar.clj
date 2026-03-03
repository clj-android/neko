(ns neko.listeners.seek-bar
  "Utility functions and macros for creating listeners corresponding to the
  android.widget.SeekBar class."
  (:require [neko.debug :refer [safe-for-ui]]))

(defn on-seek-bar-change-call
  "Takes one to three functions and yields a SeekBar.OnSeekBarChangeListener
  object that will invoke them.

  progress-fn  called when the progress level has changed.  Takes three
               arguments: seek-bar, progress (int), from-user (boolean).

  start-fn     (optional) called when the user has started a touch gesture.
               Takes one argument: seek-bar.

  stop-fn      (optional) called when the user has finished a touch gesture.
               Takes one argument: seek-bar."
  ^android.widget.SeekBar$OnSeekBarChangeListener
  [progress-fn & [start-fn stop-fn]]
  (reify android.widget.SeekBar$OnSeekBarChangeListener
    (onProgressChanged [this seek-bar progress from-user]
      (safe-for-ui (progress-fn seek-bar progress from-user)))
    (onStartTrackingTouch [this seek-bar]
      (safe-for-ui (when start-fn (start-fn seek-bar))))
    (onStopTrackingTouch [this seek-bar]
      (safe-for-ui (when stop-fn (stop-fn seek-bar))))))
