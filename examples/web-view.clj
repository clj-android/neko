(ns org.neko.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.find-view :refer [find-view]]
            [neko.threading :refer [on-ui]])
  (:import  [android.webkit WebViewClient]))

(defn make-webview-client
  "Generate a class instance of WebViewClient"
  [web-view]
  (proxy [WebViewClient] []
    (shouldOverrideUrlLoading [view url]
      (.loadUrl view url)
      true)))

(defn main-layout
  "Create a layout with a WebView"
  [activity]
  [:web-view {:id ::main-web-view
              :layout-width :fill-parent
              :layout-height :fill-parent}])

(defactivity org.neko.MainActivity
  :key :main
  :on-create (fn
               [this bundle]
               (on-ui
                (set-content-view! (*a) (main-layout (*a))))
               (on-ui
                (let [webview (find-view (*a) ::main-web-view)]
                  (doto (.getSettings webview)
                    (.setJavaScriptEnabled true)
                    (.setBuiltInZoomControls true))
                  (doto webview
                    (.loadUrl "http://clojure-android.info")
                    (.setWebViewClient (make-webview-client webview)))))))