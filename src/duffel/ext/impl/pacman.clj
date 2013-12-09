(ns duffel.ext.impl.pacman
  (:require [duffel.ext.core :refer [duffel-extension add-impl]]
            [duffel.ext.util :refer :all]
            [clojure.string  :refer [split]]
            [duffel.fs       :refer [exec exec-stream]]))

(defn- file-lines [file]
  (split (slurp file) #"\n"))

(defn- package-installed?
  [pac]
  (try (exec "sudo" "pacman" "-Q" pac) true
  (catch Exception _ false)))

(defn- install-packages!
  [util pacs & opts]
  (apply exec-stream
    "." "sudo" util
    "-Sy" "--noconfirm" "--noprogressbar" "--needed" (concat opts pacs)))

(defn- gen-process-file
  [util local & opts]
  (let [all-pacs (file-lines local)
        pacs (->> all-pacs
                  (remove #(= (first %) \#))
                  (remove empty?)
                  (remove package-installed?))]
    (if-not (empty? pacs)
      (apply install-packages! util pacs opts)
      [0 "" ""])))

(defn- maybe-throw-exception
  [util ret-code ret-stdin ret-stderr]
  (when-not (zero? ret-code)
    (throw (Exception. (str "Error when running " util ": " ret-stderr)))))

(deftype pacman-ext [] duffel-extension

  (process-dir [x app dtree]
    (throw
      (Exception. "pacman extension doesn't support handling directories")))

  (process-file [x app file-map]
    (let [opts (meta-get file-map "opts" '())
          ret (apply gen-process-file "pacman" (file-map :rel-path) opts)]
      (apply maybe-throw-exception "pacman" ret)))
)

(add-impl "pacman" (->pacman-ext))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Yaourt stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn lines-search
  [lines regex]
  (reduce (fn [_ line]
    (if-not (nil? (re-find regex line)) (reduced true) false)) lines))

(def pacman-cfg "/etc/pacman.conf")
(defn ensure-yaourt!
  "Makes sure yaourt is installed on the system, and installs it if it isn't"
  []
  (when-not (package-installed? "yaourt")

    ;Add the yaourt repo to the pacman conf if it's not already there
    (let [cfglines (file-lines pacman-cfg)]
      (when-not (lines-search cfglines #"repo.archlinux.fr")
        (println "adding archlinuxfr repo (needed to install yaourt)")
        (spit pacman-cfg
          (str "\n[archlinuxfr]\n"
               "SigLevel = Never\n"
               "Server = http://repo.archlinux.fr/$arch\n")
          :append true)))

    (let [ret (install-packages! "pacman" ["yaourt"])]
      (apply maybe-throw-exception "pacman" ret))))

(deftype yaourt-ext [] duffel-extension

  (process-dir [x app dtree]
    (throw
      (Exception. "yaourt extension doesn't support handling directories")))

  (process-file [x app file-map]
    (ensure-yaourt!)
    (let [opts (meta-get file-map "opts" '())
          ret (apply gen-process-file "yaourt" (file-map :rel-path) opts)]
      (apply maybe-throw-exception "yaourt" ret)))
)

(add-impl "yaourt" (->yaourt-ext))
