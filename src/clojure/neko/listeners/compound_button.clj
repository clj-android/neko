(ns neko.listeners.compound-button
  "Utility functions and macros for creating listeners corresponding to the
  android.widget.CompoundButton class."
  (:require [neko.debug :refer [safe-for-ui]]))

(defn on-checked-change-call
  "Takes a function and yields a CompoundButton.OnCheckedChangeListener object
  that will invoke the function.  This function must take the following two
  arguments:

  button-view  the compound button view whose state has changed
  is-checked   the new checked state of the button"
  ^android.widget.CompoundButton$OnCheckedChangeListener
  [handler-fn]
  (reify android.widget.CompoundButton$OnCheckedChangeListener
    (onCheckedChanged [this button-view is-checked]
      (safe-for-ui (handler-fn button-view is-checked)))))

(defmacro on-checked-change
  "Takes a body of expressions and yields a
  CompoundButton.OnCheckedChangeListener object that will invoke the body.
  The body takes the following implicit arguments:

  view      the compound button whose state has changed
  checked?  the new checked state of the button"
  [& body]
  `(on-checked-change-call (fn [~'view ~'checked?] ~@body)))
