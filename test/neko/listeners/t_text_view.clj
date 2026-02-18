(ns neko.listeners.t-text-view
  (:require [clojure.test :refer :all :exclude [deftest]]
            [neko.listeners.text-view :as l]
            [coa.droid-test :refer [deftest]])
  (:import [android.view KeyEvent MotionEvent]
           android.widget.TextView
           neko.App))

(defmacro test-listener [& body]
  `(let [~'v (TextView. App/instance)
         ~'called (fn [] (is true))]
     ~@body))

(deftest on-editor-action-call
  (let [test-code  KeyEvent/KEYCODE_EQUALS
        test-event (KeyEvent. KeyEvent/ACTION_DOWN test-code)]
    (test-listener
     (.onEditorAction (l/on-editor-action (called)
                                          (is (= 42 action-id))
                                          (is (= key-event test-event)))
                      v 42 test-event))

    (test-listener
     (.onEditorAction (l/on-editor-action-call (fn [_ action-id key-event]
                                                 (called)
                                                 (is (= 42 action-id))
                                                 (is (= key-event test-event))))
                      v 42 test-event))))
