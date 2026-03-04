(ns neko.ui.support.recycler-view
  "AndroidX RecyclerView widget registration and traits.

  Requires `androidx.recyclerview:recyclerview` on the classpath."
  (:require [neko.ui.mapping :as kw]
            [neko.ui.traits :refer [deftrait]]
            [neko.listeners.recycler-view :as rv-listeners])
  (:import [androidx.recyclerview.widget RecyclerView LinearLayoutManager
            GridLayoutManager StaggeredGridLayoutManager]))

(kw/defelement :recycler-view
  :classname RecyclerView
  :inherits :view-group
  :traits [:layout-manager :adapter :has-fixed-size :on-rv-scroll])

(deftrait :layout-manager
  "Takes :layout-manager attribute which can be:
  - :linear (default vertical LinearLayoutManager)
  - :linear-horizontal (horizontal LinearLayoutManager)
  - :grid followed by a span count, e.g. [:grid 2]
  - :staggered-grid followed by a span count, e.g. [:staggered-grid 2]
  - a pre-constructed LayoutManager instance"
  [^RecyclerView wdg, {:keys [layout-manager]} _]
  (let [ctx (.getContext wdg)
        lm (cond
             (instance? RecyclerView$LayoutManager layout-manager)
             layout-manager

             (= layout-manager :linear)
             (LinearLayoutManager. ctx)

             (= layout-manager :linear-horizontal)
             (LinearLayoutManager. ctx LinearLayoutManager/HORIZONTAL false)

             (and (vector? layout-manager) (= (first layout-manager) :grid))
             (GridLayoutManager. ctx (int (second layout-manager)))

             (and (vector? layout-manager) (= (first layout-manager) :staggered-grid))
             (StaggeredGridLayoutManager. (int (second layout-manager))
                                          StaggeredGridLayoutManager/VERTICAL)

             :else
             (LinearLayoutManager. ctx))]
    (.setLayoutManager wdg lm)))

(deftrait :adapter
  "Takes :adapter attribute and sets it as the RecyclerView's adapter."
  [^RecyclerView wdg, {:keys [adapter]} _]
  (.setAdapter wdg ^RecyclerView$Adapter adapter))

(deftrait :has-fixed-size
  "Takes :has-fixed-size attribute (boolean) and sets whether the RecyclerView
  has a fixed size, which allows for optimization."
  [^RecyclerView wdg, {:keys [has-fixed-size]} _]
  (.setHasFixedSize wdg (boolean has-fixed-size)))

(deftrait :on-rv-scroll
  "Takes :on-rv-scroll attribute, which should be a function of three arguments
  (recycler-view, dx, dy), and adds it as an OnScrollListener for the
  RecyclerView."
  [^RecyclerView wdg, {:keys [on-rv-scroll]} _]
  (.addOnScrollListener wdg (rv-listeners/on-scroll-call on-rv-scroll)))
