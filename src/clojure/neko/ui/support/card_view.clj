(ns neko.ui.support.card-view
  "AndroidX CardView widget registration and traits.

  Requires `androidx.cardview:cardview` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait to-dimension]])
  (:import [androidx.cardview.widget CardView]))

(kw/defelement :card-view
  :classname CardView
  :inherits :frame-layout
  :traits [:card-elevation :card-corner-radius :card-background-color])

(deftrait :card-elevation
  "Takes :card-elevation attribute (number, in pixels or dimension vector) and
  sets the CardView's elevation."
  [^CardView wdg, {:keys [card-elevation]} _]
  (.setCardElevation wdg (float (to-dimension (.getContext wdg) card-elevation))))

(deftrait :card-corner-radius
  "Takes :card-corner-radius attribute (number, in pixels or dimension vector)
  and sets the CardView's corner radius."
  [^CardView wdg, {:keys [card-corner-radius]} _]
  (.setRadius wdg (float (to-dimension (.getContext wdg) card-corner-radius))))

(deftrait :card-background-color
  "Takes :card-background-color attribute (integer color value) and sets the
  CardView's background color."
  [^CardView wdg, {:keys [card-background-color]} _]
  (.setCardBackgroundColor wdg (int card-background-color)))
