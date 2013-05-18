(ns duffel-ext.pacman
    (:use duffel.ext-protocol)
    (:require [clojure.string :as s]
              [duffel.ext     :as dext]
              [duffel.fs-util :as dfs-util]))

(defn package-installed?
    [pac]
    (try (dfs-util/exec "sudo" "pacman" "-Q" pac) true
    (catch Exception _ false)))

(defn install-packages!
    [pacs & opts]
    (let [[_ _ e] (apply dfs-util/exec-stream "." "sudo" "pacman" "-S" "--noconfirm" "--noprogressbar"
                                                         (concat opts pacs))]
        (when-not (empty? e)
            (throw (Exception. (str "Error while running pacman: " e))))))

(deftype pacman-ext [] duffel-extension

    (preprocess-file [x file-tree] file-tree)
    (preprocess-dir [x dir-tree] dir-tree)
    (file-meta-tpl [x]
        { :extra-opts '(:list (:optional ())) })
    (dir-meta-tpl [x] {})
    (postprocess-file [x file-struct] file-struct)
    (postprocess-dir [x dir-struct] dir-struct)
       
    (process-file [x app meta-struct abs local]
        (let [all-pacs (s/split (slurp local) #"\n")
              pacs (remove package-installed? all-pacs)]
            (when-not (empty? pacs) (install-packages! pacs))))


    (process-dir [x app meta-struct abs local]
        (throw (Exception. "pacman extension doesn't support handling directories")))
)

(dext/register-ext "pacman" (->pacman-ext))
