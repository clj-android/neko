(ns neko.t-reactive
  (:require [neko.ui :as ui]
            [neko.reactive :as r]
            [clojure.test :refer :all])
  (:import [android.widget Button LinearLayout TextView]
           android.view.View
           neko.App
           [org.robolectric RuntimeEnvironment]))

(set! App/instance RuntimeEnvironment/application)

(def ^:private ctx RuntimeEnvironment/application)

;; -- Unit tests for normalize-cell-value (via observable behavior) --

(deftest cell-child-single-element
  (testing "A cell containing a single UI tree renders that element"
    (let [c (r/cell [:button {:text "hello"}])
          v (ui/make-ui ctx [:linear-layout {} c])]
      (is (instance? LinearLayout v))
      (is (= 1 (.getChildCount v)))
      (is (instance? Button (.getChildAt v 0)))
      (is (= "hello" (.getText (.getChildAt v 0)))))))

(deftest cell-child-nil-value
  (testing "A cell containing nil renders no children"
    (let [c (r/cell nil)
          v (ui/make-ui ctx [:linear-layout {} c])]
      (is (= 0 (.getChildCount v))))))

(deftest cell-child-multiple-elements
  (testing "A cell containing a seq of UI trees renders all of them"
    (let [c (r/cell [[:button {:text "a"}] [:button {:text "b"}]])
          v (ui/make-ui ctx [:linear-layout {} c])]
      (is (= 2 (.getChildCount v)))
      (is (= "a" (.getText (.getChildAt v 0))))
      (is (= "b" (.getText (.getChildAt v 1)))))))

(deftest cell-child-mixed-with-static
  (testing "Cell children can be mixed with static children"
    (let [c (r/cell [:button {:text "dynamic"}])
          v (ui/make-ui ctx [:linear-layout {}
                             [:button {:text "before"}]
                             c
                             [:button {:text "after"}]])]
      (is (= 3 (.getChildCount v)))
      (is (= "before" (.getText (.getChildAt v 0))))
      (is (= "dynamic" (.getText (.getChildAt v 1))))
      (is (= "after" (.getText (.getChildAt v 2)))))))

(deftest cell-child-updates-on-reset
  (testing "Resetting a cell swaps the child views"
    (let [c (r/cell [:button {:text "v1"}])
          v (ui/make-ui ctx [:linear-layout {} c])]
      (is (= "v1" (.getText (.getChildAt v 0))))
      ;; Robolectric runs on the UI thread, so the watch fires synchronously
      (reset! c [:text-view {:text "v2"}])
      (is (= 1 (.getChildCount v)))
      (is (instance? TextView (.getChildAt v 0)))
      (is (= "v2" (.getText (.getChildAt v 0)))))))

(deftest cell-child-update-to-nil
  (testing "Setting a cell to nil removes all its children"
    (let [c (r/cell [:button {:text "temp"}])
          v (ui/make-ui ctx [:linear-layout {} c])]
      (is (= 1 (.getChildCount v)))
      (reset! c nil)
      (is (= 0 (.getChildCount v))))))

(deftest cell-child-update-from-nil
  (testing "Setting a cell from nil to a value adds children"
    (let [c (r/cell nil)
          v (ui/make-ui ctx [:linear-layout {} c])]
      (is (= 0 (.getChildCount v)))
      (reset! c [:button {:text "appeared"}])
      (is (= 1 (.getChildCount v)))
      (is (= "appeared" (.getText (.getChildAt v 0)))))))

(deftest cell-child-preserves-position
  (testing "Cell update inserts new views at the same position"
    (let [c (r/cell [:button {:text "middle"}])
          v (ui/make-ui ctx [:linear-layout {}
                             [:button {:text "first"}]
                             c
                             [:button {:text "last"}]])]
      (is (= "first" (.getText (.getChildAt v 0))))
      (is (= "middle" (.getText (.getChildAt v 1))))
      (is (= "last" (.getText (.getChildAt v 2))))
      ;; Update the cell
      (reset! c [:text-view {:text "replaced"}])
      (is (= 3 (.getChildCount v)))
      (is (= "first" (.getText (.getChildAt v 0))))
      (is (= "replaced" (.getText (.getChildAt v 1))))
      (is (= "last" (.getText (.getChildAt v 2)))))))

(deftest cell-child-expand-count
  (testing "Cell can change from 1 child to multiple children"
    (let [c (r/cell [:button {:text "one"}])
          v (ui/make-ui ctx [:linear-layout {}
                             [:button {:text "static"}]
                             c])]
      (is (= 2 (.getChildCount v)))
      (reset! c [[:button {:text "a"}] [:button {:text "b"}] [:button {:text "c"}]])
      (is (= 4 (.getChildCount v)))
      (is (= "static" (.getText (.getChildAt v 0))))
      (is (= "a" (.getText (.getChildAt v 1))))
      (is (= "b" (.getText (.getChildAt v 2))))
      (is (= "c" (.getText (.getChildAt v 3)))))))

(deftest cell-child-shrink-count
  (testing "Cell can change from multiple children to fewer"
    (let [c (r/cell [[:button {:text "a"}] [:button {:text "b"}]])
          v (ui/make-ui ctx [:linear-layout {} c])]
      (is (= 2 (.getChildCount v)))
      (reset! c [:button {:text "only"}])
      (is (= 1 (.getChildCount v)))
      (is (= "only" (.getText (.getChildAt v 0)))))))

(deftest formula-cell-child
  (testing "A formula cell (cell=) works as a child"
    (let [flag (r/cell true)
          content (r/cell= #(if @flag
                              [:button {:text "on"}]
                              [:text-view {:text "off"}]))
          v (ui/make-ui ctx [:linear-layout {} content])]
      (is (instance? Button (.getChildAt v 0)))
      (is (= "on" (.getText (.getChildAt v 0))))
      (reset! flag false)
      (is (instance? TextView (.getChildAt v 0)))
      (is (= "off" (.getText (.getChildAt v 0)))))))

(deftest multiple-cell-children
  (testing "Multiple cell children in the same parent work independently"
    (let [c1 (r/cell [:button {:text "c1"}])
          c2 (r/cell [:button {:text "c2"}])
          v (ui/make-ui ctx [:linear-layout {} c1 c2])]
      (is (= 2 (.getChildCount v)))
      (is (= "c1" (.getText (.getChildAt v 0))))
      (is (= "c2" (.getText (.getChildAt v 1))))
      ;; Update only c1
      (reset! c1 [:text-view {:text "c1-new"}])
      (is (= 2 (.getChildCount v)))
      (is (= "c1-new" (.getText (.getChildAt v 0))))
      (is (= "c2" (.getText (.getChildAt v 1))))
      ;; Update only c2
      (reset! c2 [:text-view {:text "c2-new"}])
      (is (= "c1-new" (.getText (.getChildAt v 0))))
      (is (= "c2-new" (.getText (.getChildAt v 1)))))))

(deftest cell-child-raw-view
  (testing "A cell can hold a pre-built View object"
    (let [btn (doto (Button. ctx) (.setText "raw"))
          c (r/cell nil)
          v (ui/make-ui ctx [:linear-layout {} c])]
      (is (= 0 (.getChildCount v)))
      (reset! c btn)
      (is (= 1 (.getChildCount v)))
      (is (identical? btn (.getChildAt v 0))))))

;; -- Verify existing behavior still works --

(deftest static-children-still-work
  (testing "Non-cell children work exactly as before"
    (let [v (ui/make-ui ctx [:linear-layout {}
                             [:button {:text "a"}]
                             [:button {:text "b"}]])]
      (is (= 2 (.getChildCount v)))
      (is (= "a" (.getText (.getChildAt v 0))))
      (is (= "b" (.getText (.getChildAt v 1)))))))

(deftest nil-children-still-filtered
  (testing "nil children are still silently skipped"
    (let [v (ui/make-ui ctx [:linear-layout {}
                             nil
                             [:button {:text "ok"}]
                             nil])]
      (is (= 1 (.getChildCount v)))
      (is (= "ok" (.getText (.getChildAt v 0)))))))
