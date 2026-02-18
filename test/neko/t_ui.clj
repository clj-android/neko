(ns neko.t-ui
  (:require [neko.ui :as ui]
            [neko.activity :refer [defactivity]]
            [neko.internal :as utils]
            [clojure.test :refer :all])
  (:import [android.widget Button LinearLayout TextView]
           android.content.pm.ActivityInfo
           android.view.View
           neko.App
           [org.robolectric Robolectric RuntimeEnvironment]))

(deftest apply-default-setters-from-attributes
  (let [v (Button. RuntimeEnvironment/application)]
    (is (= View/VISIBLE (.getVisibility v)))
    (is (.isEnabled v))
    (ui/apply-default-setters-from-attributes
     :button v {:visibility View/GONE, :enabled false})
    (is (not (.isEnabled v)))
    (is (=  View/GONE (.getVisibility v)))))

(deftest apply-attributes
  (let [v (Button. RuntimeEnvironment/application)]
    (is (.isEnabled v))
    (is (= "" (.getText v)))
    (ui/apply-attributes :button v
                         {:enabled false, :text "hello"} {})
    (is (not (.isEnabled v)))
    (is (= "hello" (.getText v))))

  (let [ll (LinearLayout. RuntimeEnvironment/application)]
    (is (= {:container-type :linear-layout
            :id-holder ll}
           (ui/apply-attributes :linear-layout ll {:id-holder true} {})))))

(deftest construct-element
  (is (instance? Button (ui/construct-element
                         :button RuntimeEnvironment/application []))))

(deftest make-ui-element
  (is (thrown? AssertionError (ui/make-ui-element RuntimeEnvironment/application
                                                  ['button {:foo "bar"}] {})))
  (is (thrown? AssertionError (ui/make-ui-element RuntimeEnvironment/application
                                                  [:button [:foo "bar"]] {})))
  (let [v (Button. RuntimeEnvironment/application)]
    (= v (ui/make-ui-element RuntimeEnvironment/application v {})))

  (let [v (ui/make-ui-element RuntimeEnvironment/application
                              [:button {:text "hello"}] {})]
    (instance? Button v)
    (is (= "hello" (.getText v))))

  (let [v (ui/make-ui-element RuntimeEnvironment/application
                              [:button {:custom-constructor
                                        (fn [ctx] (doto (Button. ctx)
                                                   (.setVisibility View/GONE)))}]
                              {})]
    (instance? Button v)
    (is (= View/GONE (.getVisibility v)))))

(deftest make-ui
  (let [v (ui/make-ui RuntimeEnvironment/application
                      [:linear-layout {:id-holder true
                                       :orientation :vertical}
                       [:button {:id ::hello
                                 :text "hello"}]])]
    (is (instance? LinearLayout v))
    (is (= LinearLayout/VERTICAL (.getOrientation v)))
    (let [tag (.getTag v)
          b (get tag ::hello)]
      (is (instance? Button b))
      (is (= (utils/int-id ::hello) (.getId b)))
      (is (= "hello" (.getText b))))))

(deftest config
  (let [v (ui/make-ui RuntimeEnvironment/application [:button {:text "hello"}])]
    (is (= View/VISIBLE (.getVisibility v)))
    (is (= "hello" (.getText v)))
    (ui/config v :text "updated" :visibility View/GONE)
    (is (= "updated" (.getText v)))
    (is (= View/GONE (.getVisibility v)))))

(deftest inflate-layout
  (is (instance? TextView (ui/inflate-layout RuntimeEnvironment/application
                                             android.R$layout/simple_list_item_1))))

(set! App/instance RuntimeEnvironment/application)

(deftest get-screen-orientation
  ;; Robolectric always returns :undefined on orientation, oh well
  (is (= :undefined (ui/get-screen-orientation RuntimeEnvironment/application)))
  (is (= :undefined (ui/get-screen-orientation))))
