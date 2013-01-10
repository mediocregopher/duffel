(ns duffel-ext.ignore
    (:use duffel.ext-protocol)
    (:require [duffel.ext     :as dext]))

(deftype ignore-ext [] duffel-extension

    (preprocess-file [x file-tree] file-tree)
    (preprocess-dir [x dir-tree] dir-tree)
    (file-meta-tpl [x] {})
    (dir-meta-tpl [x] {})
    (postprocess-file [x file-struct] file-struct)
    (postprocess-dir [x dir-struct] dir-struct)
    (process-file [x app meta-struct abs local]
        (println "Ignoring" abs))
    (process-dir [x app meta-struct abs local]
        (println "Ignoring" abs))
)

(dext/register-ext "ignore" (->ignore-ext))
