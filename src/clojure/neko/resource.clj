(ns neko.resource
  "Provides utilities to resolve application resources."
  (:require [neko.internal :refer [app-package-name]])
  (:import android.content.Context
           android.graphics.drawable.Drawable
           android.util.TypedValue
           neko.App))

(defmacro import-all
  "Imports all existing application's R subclasses (R$drawable, R$string etc.)
  into the current namespace."
  []
  `(do ~@(map (fn [res-type]
                `(try (import '~(-> (app-package-name)
                                    (str ".R$" res-type)
                                    symbol))
                      (catch ClassNotFoundException _# nil)))
              '[anim drawable color layout menu string array plurals style id
                dimen raw])))

(import-all)
;; ## Runtime resource resolution

(defn get-string
  "Gets the localized string for the given resource ID. If res-name is a string,
  returns it unchanged. If additional arguments are supplied, the string will be
  interpreted as a format and the arguments will be applied to the format."
  {:forms '([res-id & format-args?] [context res-id & format-args?])}
  [& args]
  (let [[^Context context args] (if (instance? Context (first args))
                                  [(first args) (rest args)]
                                  [App/instance args])
        [res-id & format-args] args]
    (cond (not (number? res-id)) res-id
          format-args      (.getString context res-id (to-array format-args))
          :else            (.getString context res-id))))

(defn get-drawable
  "Gets a Drawable object associated with the given resource ID. If res-id is a
  Drawable, returns it unchanged."
  ([res-id]
   (get-drawable App/instance res-id))

  ([^Context context, res-id]
   (if-not (number? res-id)
     res-id
     (.getDrawable (.getResources context) res-id))))

;; ## Theme color resolution

(defn- get-attr-field
  "Looks up a static int field from a class by name using reflection.
  Returns nil if the class or field is not found (e.g. Material not on classpath)."
  [^String class-name ^String field-name]
  (try
    (-> (Class/forName class-name) (.getField field-name) (.get nil))
    (catch Exception _ nil)))

(def ^:private theme-color-attrs
  "Lazily-initialized map from keywords to Material / Android theme attribute
  resource IDs. Material attributes are resolved at runtime via reflection so
  that neko compiles even when com.google.android.material is absent from the
  AOT classpath."
  (delay
    (let [mat #(get-attr-field "com.google.android.material.R$attr" %)]
      (into {} (filter val
                 {:color-primary                android.R$attr/colorPrimary
                  :color-on-primary             (mat "colorOnPrimary")
                  :color-primary-container      (mat "colorPrimaryContainer")
                  :color-on-primary-container   (mat "colorOnPrimaryContainer")
                  :color-secondary              (mat "colorSecondary")
                  :color-on-secondary           (mat "colorOnSecondary")
                  :color-secondary-container    (mat "colorSecondaryContainer")
                  :color-on-secondary-container (mat "colorOnSecondaryContainer")
                  :color-tertiary               (mat "colorTertiary")
                  :color-on-tertiary            (mat "colorOnTertiary")
                  :color-tertiary-container     (mat "colorTertiaryContainer")
                  :color-on-tertiary-container  (mat "colorOnTertiaryContainer")
                  :color-surface                (mat "colorSurface")
                  :color-on-surface             (mat "colorOnSurface")
                  :color-surface-variant        (mat "colorSurfaceVariant")
                  :color-on-surface-variant     (mat "colorOnSurfaceVariant")
                  :color-outline                (mat "colorOutline")
                  :color-outline-variant        (mat "colorOutlineVariant")
                  :color-error                  (mat "colorError")
                  :color-on-error               (mat "colorOnError")
                  :color-error-container        (mat "colorErrorContainer")
                  :color-on-error-container     (mat "colorOnErrorContainer")
                  :color-background             android.R$attr/colorBackground
                  :color-on-background          (mat "colorOnBackground")})))))

(defn get-theme-color
  "Resolves a color from the current Activity/Context theme.

  attr can be:
  - A keyword from the named set (e.g. :color-primary, :color-surface, :text-color-primary).
    See neko.resource/theme-color-attrs for the full list.
  - An integer attribute resource ID (e.g. android.R$attr/colorPrimary).

  Returns the resolved color as an ARGB int, or default-color (default 0) if
  the attribute is not defined in the theme.

  Usage:
    (get-theme-color activity :color-primary)
    (get-theme-color :color-surface)
    (get-theme-color activity android.R$attr/colorBackground)"
  {:forms '([attr] [context attr] [context attr default-color])}
  [& args]
  (let [[^Context context args] (if (instance? Context (first args))
                                  [(first args) (rest args)]
                                  [App/instance args])
        [attr & rest-args]      args
        default-color           (or (first rest-args) 0)
        attr-id                 (if (keyword? attr)
                                  (get @theme-color-attrs attr)
                                  attr)]
    (when attr-id
      (let [tv (TypedValue.)]
        (if (.resolveAttribute (.getTheme context) attr-id tv true)
          (.data tv)
          default-color)))))
