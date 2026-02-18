(ns neko.t-activity
  (:require [clojure.test :refer :all :exclude [deftest]]
            [neko.activity :refer [defactivity] :as a]
            [neko.debug :as dbg]
            [neko.ui :as ui]
            [neko.find-view :refer [find-view]]
            [coa.droid-test :refer [deftest]])
  (:import android.app.Activity
           android.os.Bundle
           android.view.View
           coa.droid_test.Helpers
           [android.widget LinearLayout TextView]
           [org.robolectric Robolectric RuntimeEnvironment]
           [org.robolectric.util ActivityController ComponentController]))

(def simple-ui [:linear-layout {:orientation :vertical}
                [:text-view {:id ::tv
                             :text "test"}]])

(defn top-level-view [activity]
  (-> (a/get-decor-view activity)
      (.findViewById android.R$id/content)
      (.getChildAt 0)))

(defn make-activity []
  (Robolectric/setupActivity Activity))

(deftest set-content-view
  (testing "set View objects"
    (let [activity (make-activity)
          view (View. RuntimeEnvironment/application)]
      (a/set-content-view! activity view)
      (is (= view (top-level-view activity)))))

  (testing "set layout IDs"
    (let [activity (make-activity)]
      (a/set-content-view! activity android.R$layout/simple_list_item_1)
      (is (= TextView (type (.findViewById activity android.R$id/text1))))))

  (testing "set neko.ui trees"
    (let [activity (make-activity)
          neko-view (ui/make-ui RuntimeEnvironment/application simple-ui)]
      (is (nil? (find-view activity ::tv)))
      (a/set-content-view! activity simple-ui)
      (is (= TextView (type (find-view activity ::tv)))))))

(deftest request-window-features
  (testing "empty"
    (let [activity (make-activity)]
      (is (= [] (a/request-window-features! activity)))))

  (testing "one feature"
    (defactivity neko.TestActivity
      (onCreate [this bundle]
        (.superOnCreate this bundle)
        (is (= [true] (a/request-window-features! this :progress)))))
    (Robolectric/setupActivity neko.TestActivity))

  (testing "multiple features"
    (defactivity neko.TestActivity
      (onCreate [this bundle]
        (.superOnCreate this bundle)
        (is (= [true true] (a/request-window-features! this :progress :no-title)))))
    (Robolectric/setupActivity neko.TestActivity)))

(definterface TestInterface
  (ifaceMethod []))

(deftest defactivity-tests
  (defactivity neko.DefActivity
    :implements [neko.t_activity.TestInterface]
    :key :defact
    :request-features [:no-title :progress]

    (onCreate [this bundle]
      (.superOnCreate this bundle)
      (is (instance? Activity this))
      (is (= this (get dbg/all-activities :defact)))
      (is (= this (get dbg/all-activities 'neko.t-activity)))
      (is (= {} @(a/get-state this)))
      (swap! (a/get-state this) assoc :test "test"))

    (onStart [this]
      (.superOnStart this)
      (is (= "test" (:test @(a/get-state this)))))

    (onStop [this]
      (.superOnStop this)
      (is true))

    (ifaceMethod [this]
      (is true)))

  (let [controller (-> (Robolectric/buildActivity neko.DefActivity)
                       .setup .stop)
        activity (Helpers/getActivity controller)]
    (.ifaceMethod activity)))
