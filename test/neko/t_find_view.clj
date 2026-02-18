(ns neko.t-find-view
  (:require [clojure.test :refer :all]
            [neko.find-view :as fv]
            [neko.ui :as ui])
  (:import [android.widget TextView EditText Button]
           org.robolectric.RuntimeEnvironment
           neko.App))

(def simple-ui [:linear-layout {:id-holder true
                                :orientation :vertical}
                [:relative-layout {:id ::rel
                                   :id-holder true}
                 [:text-view {:id ::tv-in-rel
                              :text "test"}]
                 [:edit-text {:id ::et-in-rel
                              :hint "test"}]]
                [:button {:id ::but
                          :text "Button"}]])

(deftest find-view
  (let [view (ui/make-ui RuntimeEnvironment/application simple-ui)]
    (is (instance? Button (fv/find-view view ::but)))
    (is (nil? (fv/find-view view ::tv-in-rel)))

    (let [rel (fv/find-view view ::rel)
          [tv et] (fv/find-views rel ::tv-in-rel ::et-in-rel)]
      (is (instance? TextView tv))
      (is (instance? EditText et)))))

