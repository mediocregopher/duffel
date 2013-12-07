(ns duffel.ext.impl.pacman
    (:use duffel.ext-protocol)
    (:require [clojure.string :as s]
              [duffel.ext     :as dext]
              [duffel.fs-util :as dfs-util]))

(defn- file-lines [file]
    (s/split (slurp file) #"\n"))

(defn package-installed?
    [pac]
    (try (dfs-util/exec "sudo" "pacman" "-Q" pac) true
    (catch Exception _ false)))

(defn- install-packages!
    [util pacs & opts]
    (apply dfs-util/exec-stream
      "." "sudo" util
      "-Sy" "--noconfirm" "--noprogressbar" "--needed" (concat opts pacs)))

(def tpl { :extra-opts '( :list (:optional ()) (:map-tpl (:string)) ) })

(defn- gen-process-file
    [util meta-struct local]
    (let [all-pacs (file-lines local)
          pacs (->> all-pacs
                    (remove package-installed?)
                    (remove empty?)
                    (remove #(= (first %) \#)))]
        (if-not (empty? pacs)
          (apply install-packages! util pacs (meta-struct :extra-opts))
          [0 "" ""])))

(defn- maybe-throw-exception
  [util ret-code ret-stdin ret-stderr]
  (when-not (zero? ret-code)
    (throw (Exception. (str "Error when running " util ": " ret-stderr)))))

(deftype pacman-ext [] duffel-extension
    (preprocess-file [x file-tree] file-tree)
    (preprocess-dir [x dir-tree] dir-tree)
    (file-meta-tpl [x] tpl)
    (dir-meta-tpl [x] {})
    (postprocess-file [x file-struct] file-struct)
    (postprocess-dir [x dir-struct] dir-struct)

    (process-file [x app meta-struct abs local]
      (let [ret (gen-process-file "pacman" meta-struct local)]
        (apply maybe-throw-exception "pacman" ret)))

    (process-dir [x app meta-struct abs local]
        (throw
          (Exception. "pacman extension doesn't support handling directories")))
)
(dext/register-ext "pacman" (->pacman-ext))

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
    (preprocess-file [x file-tree] file-tree)
    (preprocess-dir [x dir-tree] dir-tree)
    (file-meta-tpl [x] tpl)
    (dir-meta-tpl [x] {})
    (postprocess-file [x file-struct] file-struct)
    (postprocess-dir [x dir-struct] dir-struct)

    (process-file [x app meta-struct abs local]
      (ensure-yaourt!)
      (let [ret (gen-process-file "yaourt" meta-struct local)]
        (apply maybe-throw-exception "yaourt" ret)))

    (process-dir [x app meta-struct abs local]
        (throw
          (Exception. "yaourt extension doesn't support handling directories")))
)
(dext/register-ext "yaourt" (->yaourt-ext))
