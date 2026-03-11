(ns neko.ui.support.window-insets
  "Edge-to-edge window inset support via AndroidX ViewCompat.

  Requires `androidx.core:core` on the classpath (already a compileOnly
  dependency in neko; the consuming app must declare it as `implementation`).

  ## Typical usage

  In your activity's on-create, before setContentView:

    (neko.ui.support.window-insets/enable-edge-to-edge! activity)

  In your UI tree, annotate views that should respond to system bar insets:

    [:linear-layout {:insets-padding :top}]        ; header — pads by status bar height
    [:recycler-view {:insets-padding :bottom        ; content — pads by nav bar height
                     :clip-to-padding true}]

  ## Traits

  `:insets-padding` — adds system bar insets as *additional* padding on the
  specified sides. Existing padding (from `:padding`) is preserved as the
  baseline; inset pixels are added on top each time insets are dispatched.

  `:insets-margin` — same as `:insets-padding` but adjusts layout margins.
  Requires the view's LayoutParams to be a MarginLayoutParams subclass (true
  for LinearLayout, FrameLayout, RelativeLayout, ConstraintLayout, etc.).
  Silently no-ops if the LayoutParams are not a MarginLayoutParams.

  ## Side values (apply to both traits)

    :top, :bottom, :left, :right   — a single side
    #{:top :bottom}                — a set of sides
    :all or true                   — all four sides
    :start, :end                   — aliases for :left, :right

  ## Companion attributes (apply to both traits)

  `:inset-types` — which WindowInsetsCompat types to use.
    Default: #{:system-bars :display-cutout} (the recommended combination).
    Options: :system-bars, :display-cutout, :ime, :system-gestures,
             :mandatory-system-gestures, :tappable-element.
    Accepts a single keyword or a set of keywords.

  `:consume-insets` — when true, returns WindowInsetsCompat.CONSUMED after
    handling, so child views do not also receive the insets. Default: false."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]])
  (:import [androidx.core.view ViewCompat WindowCompat
            WindowInsetsCompat WindowInsetsCompat$Type]
           [android.view View ViewGroup]
           android.app.Activity))

;; ---------------------------------------------------------------------------
;; Public utility functions
;; ---------------------------------------------------------------------------

(defn enable-edge-to-edge!
  "Enables edge-to-edge display for the given activity's window.
  Makes the app window extend behind the status and navigation bars.
  Call this from onCreate, before setContentView."
  [^Activity activity]
  (WindowCompat/setDecorFitsSystemWindows (.getWindow activity) false))

;; ---------------------------------------------------------------------------
;; Internal helpers
;; ---------------------------------------------------------------------------

(def ^:private inset-type-map
  {:system-bars               (WindowInsetsCompat$Type/systemBars)
   :display-cutout            (WindowInsetsCompat$Type/displayCutout)
   :ime                       (WindowInsetsCompat$Type/ime)
   :system-gestures           (WindowInsetsCompat$Type/systemGestures)
   :mandatory-system-gestures (WindowInsetsCompat$Type/mandatorySystemGestures)
   :tappable-element          (WindowInsetsCompat$Type/tappableElement)})

(defn- inset-type-flags
  "Converts a keyword or set of keywords to a combined type bitmask."
  [types]
  (let [type-set (if (keyword? types) #{types} (set types))]
    (reduce bit-or 0 (keep inset-type-map type-set))))

(defn- normalize-sides
  "Converts a sides value to a canonical set of :top/:bottom/:left/:right."
  [sides]
  (let [raw (cond
              (or (= sides :all) (true? sides)) #{:top :bottom :left :right}
              (keyword? sides) #{sides}
              :else (set sides))]
    (reduce (fn [s side]
              (case side
                :start (conj s :left)
                :end   (conj s :right)
                (conj s side)))
            #{} raw)))

;; ---------------------------------------------------------------------------
;; Traits
;; ---------------------------------------------------------------------------

(deftrait :insets-padding
  "Registers a WindowInsetsCompat listener that adjusts the view's padding
  when system bar insets are dispatched. The padding from :padding attributes
  is used as a baseline; inset values are added on top.

  Value: :top, :bottom, :left, :right; a set thereof; or :all / true."
  {:attributes [:insets-padding :inset-types :consume-insets]
   :applies? (some? insets-padding)}
  [^View wdg {:keys [insets-padding inset-types consume-insets]} _]
  (let [sides  (normalize-sides insets-padding)
        flags  (inset-type-flags (or inset-types #{:system-bars :display-cutout}))
        base-l (.getPaddingLeft wdg)
        base-t (.getPaddingTop wdg)
        base-r (.getPaddingRight wdg)
        base-b (.getPaddingBottom wdg)]
    (ViewCompat/setOnApplyWindowInsetsListener
      wdg
      (reify androidx.core.view.OnApplyWindowInsetsListener
        (onApplyWindowInsets [_ v insets]
          (let [ins (.getInsets insets flags)]
            (.setPadding v
              (if (:left sides)   (+ base-l (.left ins))   base-l)
              (if (:top sides)    (+ base-t (.top ins))    base-t)
              (if (:right sides)  (+ base-r (.right ins))  base-r)
              (if (:bottom sides) (+ base-b (.bottom ins)) base-b)))
          (if consume-insets WindowInsetsCompat/CONSUMED insets))))))

(deftrait :insets-margin
  "Registers a WindowInsetsCompat listener that adjusts the view's layout
  margins when system bar insets are dispatched. Base margins (from
  :layout-margin attributes) are preserved as the baseline. Requires
  MarginLayoutParams; silently no-ops otherwise.

  Value: :top, :bottom, :left, :right; a set thereof; or :all / true."
  {:attributes [:insets-margin :inset-types :consume-insets]
   :applies? (some? insets-margin)}
  [^View wdg {:keys [insets-margin inset-types consume-insets]} _]
  (when (instance? android.view.ViewGroup$MarginLayoutParams (.getLayoutParams wdg))
    (let [sides  (normalize-sides insets-margin)
          flags  (inset-type-flags (or inset-types #{:system-bars :display-cutout}))
          ^android.view.ViewGroup$MarginLayoutParams mlp (.getLayoutParams wdg)
          base-l (.-leftMargin mlp)
          base-t (.-topMargin mlp)
          base-r (.-rightMargin mlp)
          base-b (.-bottomMargin mlp)]
      (ViewCompat/setOnApplyWindowInsetsListener
        wdg
        (reify androidx.core.view.OnApplyWindowInsetsListener
          (onApplyWindowInsets [_ v insets]
            (let [ins (.getInsets insets flags)
                  ^android.view.ViewGroup$MarginLayoutParams lp (.getLayoutParams v)]
              (.setMargins lp
                (if (:left sides)   (+ base-l (.left ins))   base-l)
                (if (:top sides)    (+ base-t (.top ins))    base-t)
                (if (:right sides)  (+ base-r (.right ins))  base-r)
                (if (:bottom sides) (+ base-b (.bottom ins)) base-b))
              (.setLayoutParams v lp))
            (if consume-insets WindowInsetsCompat/CONSUMED insets)))))))

;; Register traits globally on :view so all widgets can use them
(kw/add-trait! :view :insets-padding)
(kw/add-trait! :view :insets-margin)
