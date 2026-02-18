(defproject neko "4.0.0-alpha5"
  :description "Neko is a toolkit designed to make Android development using Clojure easier and more fun."
  :url "https://github.com/clojure-android/neko"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure-android/clojure "1.7.0-r2"]
                 [com.android.support/multidex "1.0.0" :extension "aar"]]
  :plugins [[lein-droid "0.4.6"]]

  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :profiles {:default [:android-common]

             :robolectric
             [:android-common
              {:dependencies [[junit/junit "4.12"]
                              [org.robolectric/robolectric "3.0"]
                              [org.clojure-android/droid-test "0.1.1-SNAPSHOT"]
                              [org.clojure/tools.nrepl "0.2.10"]]}]

             :local-testing
             [:robolectric
              {:target-path "target/local-testing"
               :dependencies [[venantius/ultra "0.3.3"]]
               :android {:aot [#"neko.*\.t-.+" "ultra.test"]}}]

             :local-repl
             [:robolectric
              {:target-path "target/local-repl"
               :android {:aot :all-with-unused}}]

             :travis
             [:local-testing
              {:dependencies [[cloverage "1.0.6" :exclusions [org.clojure/tools.logging]]
                              [org.clojure-android/tools.logging "0.3.2-r1"]]
               :plugins [[lein-shell "0.4.0"]]
               :aliases {"coverage" ["do" ["droid" "local-test" "cloverage"]
                                     ["shell" "curl" "-F"
                                      "json_file=@target/coverage/coveralls.json"
                                      "https://coveralls.io/api/v1/jobs"]]}
               :android {:sdk-path "/usr/local/android-sdk/"
                         :aot ["cloverage.coverage"]
                         :cloverage-exclude-ns ["neko.tools.repl"]}}]}


  :android {:library true
            :target-version 18})
