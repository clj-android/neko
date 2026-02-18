(ns neko.ui.t-listview
  (:require [neko.ui.listview :as lv]
            [neko.ui :as ui]
            [neko.ui.adapters :refer [ref-adapter]]
            [clojure.test :refer :all])
  (:import [android.widget ListView CheckBox]
           neko.App))

(deftest get-checked
  (let [ref (atom (range 5))
        adapter (ref-adapter (fn [c] (CheckBox. c))
                             (fn [pos v _ data] (.setText v (str data)))
                             ref)
        v (ui/make-ui App/instance
                      [:list-view {:adapter adapter
                                   :choice-mode ListView/CHOICE_MODE_MULTIPLE}])]
    (is (= [] (lv/get-checked v)))
    (.setItemChecked v 1 true)
    (.setItemChecked v 3 true)
    (is (= [1 3] (lv/get-checked v)))
    (is (= ["one" "three"] (lv/get-checked v ["zero" "one" "two" "three" "four"])))))

(deftest set-checked
  (let [ref (atom (range 10))
        adapter (ref-adapter (fn [c] (CheckBox. c))
                             (fn [pos v _ data] (.setText v (str data)))
                             ref)
        v (ui/make-ui App/instance
                      [:list-view {:adapter adapter
                                   :choice-mode ListView/CHOICE_MODE_MULTIPLE}])]
    (is (= [] (lv/get-checked v)))
    (lv/set-checked! v [4 5 6])
    (is (= [4 5 6] (lv/get-checked v)))))
