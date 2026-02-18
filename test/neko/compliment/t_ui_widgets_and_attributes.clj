(ns neko.compliment.t-ui-widgets-and-attributes
  (:require [neko.compliment.ui-widgets-and-attributes :as comp]
            [clojure.test :refer :all]))

(deftest candidates
  (let [widget-ctx '[{:form [__prefix__ {}], :idx 0}]
        attr-ctx '[{:form {__prefix__ nil}, :idx nil, :map-role :key}
                   {:form [:text-view {__prefix__ nil}], :idx 1}]
        attr2-ctx '[{:form {__prefix__ nil}, :idx nil, :map-role :key}
                    {:form [:nothing {__prefix__ nil}], :idx 1}]
        bad-ctx '[{:form {:text __prefix__}, :idx nil, :map-role :val}
                  {:form [:text-view {:text __prefix__}], :idx 1}]
        include (fn [subelems list] (every? (fn [x] (some #(= % x) list)) subelems))]
    (testing "widgets"
      (is (include [":view" ":view-group"] (comp/candidates ":vi" *ns* widget-ctx)))
      (is (include [":progress-bar"] (comp/candidates ":progress" *ns* widget-ctx)))
      (is (= [] (comp/candidates ":bollocks" *ns* widget-ctx))))

    (testing "attributes"
      (is (include [":text" ":text-size"] (comp/candidates ":te" *ns* attr-ctx)))
      (is (include [":layout-margin-left" ":layout-margin" ":layout-margin-top"
                    ":layout-margin-right" ":layout-margin-bottom"]
                   (comp/candidates ":layout-marg" *ns* attr-ctx)))
      ;; Unparsed widget-kw yields all attributes
      (is (= [":image"] (comp/candidates ":ima" *ns* attr2-ctx)))
      ;; Wrong widget kw
      (is (= [] (comp/candidates ":image" *ns* attr-ctx)))
      )

    (testing "bad-context"
      (is (= [] (comp/candidates ":text" *ns* bad-ctx)))
      (is (= [] (comp/candidates ":vi" *ns* bad-ctx))))))

(deftest docs
  (is (re-matches #"(?s):text-view - android.widget.TextView\n.+"
                  (comp/doc ":text-view" *ns*)))
  (is (re-matches #"(?s)^:id-holder -.+"  (comp/doc ":id-holder" *ns*)))
  (is (re-matches #"(?s)^:linear-layout-params - .+"  (comp/doc ":layout-weight" *ns*)))
  (is (= nil (comp/doc ":unrelated" *ns*))))

(deftest initialization
  (testing "wouldn't die without compliment"
    (is (= nil (comp/init-source)))))
