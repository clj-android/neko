(ns neko.action-bar
  "Provides utilities to manipulate application ActionBar."
  (:require neko.ui)
  (:use [neko.ui.mapping :only [defelement]]
        [neko.ui.traits :only [deftrait]])
  (:import android.app.ActionBar
           android.app.Activity))

;; ## Functions for declarative definition

(defelement :action-bar
  :classname android.app.ActionBar
  :inherits nil
  :traits [:display-options])

(defn display-options-value
  "Returns an integer value for the given keyword, or the value itself."
  [value]
  (if (keyword? value)
    (case value
      :home-as-up  ActionBar/DISPLAY_HOME_AS_UP
      :show-home   ActionBar/DISPLAY_SHOW_HOME
      :show-custom ActionBar/DISPLAY_SHOW_CUSTOM
      :show-title  ActionBar/DISPLAY_SHOW_TITLE
      :use-logo    ActionBar/DISPLAY_USE_LOGO)
    value))

(deftrait :display-options
  "Takes `:display-options` attribute, which could be an integer value
  or one of the following keywords: `:home-as-up`, `:show-home`,
  `:show-custom`, `:show-title`, `:use-logo`, or a vector with these
  values, to which bit-or operation will be applied."
  [^ActionBar action-bar, {:keys [display-options]} _]
  (let [value (if (vector? display-options)
                (apply bit-or (map display-options-value display-options))
                (display-options-value display-options))]
    (.setDisplayOptions action-bar value)))

(defn setup-action-bar
  "Configures activity's action bar according to the attributes
  provided in key-value fashion. For more information,
  see `(describe :action-bar)`."
  [^Activity activity, attributes-map]
  (let [action-bar (.getActionBar activity)]
    (neko.ui/apply-attributes :action-bar action-bar attributes-map {})))
