(defproject osbert/gh-fork-explorer "0.1.0"
  :description "Search through commits on GitHub forks."
  :url "http://github.com/osbert/gh-fork-explorer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojars.osbert/tentacles "0.2.6"]
                 [clucy "0.4.0"]
                 [compojure "1.1.5" :exclusions [org.clojure/clojure ring/ring-core]]
                 [hiccup "1.0.3" :exclusions [org.clojure/clojure]]
                 [ring-server "0.2.8" :exclusions [org.clojure/clojure]]
                 [org.clojars.osbert/hiccup-bootstrap "0.1.2-SNAPSHOT" :exclusions [compojure org.clojure/clojure hiccup]]
                 [cssgen "0.3.0-alpha1" :exclusions [org.clojure/clojure]]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.3"]]}})
