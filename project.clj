(defproject duffel "1.1.1"
  :description "Duffel is a standalone executable which will automatically deploy a directory structure to your machine"
  :url "https://github.com/mediocregopher/duffel"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojars.jhowarth/clojure-contrib "1.2.0-RC3"]
                 [org.clojars.mediocregopher/clerb "0.2"]
                 [cheshire "5.0.1"]]
  :main duffel.core)
