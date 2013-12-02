(ns duffel-ext.dlns
    (:use duffel.ext-protocol)
    (:require [duffel.fs-util  :as dfs-util]
              [duffel.ext      :as dext]
              [duffel.ext-util :as dext-util]))

(defn ln-file [src dest meta]
  (let [src (dfs-util/get-full-path src)]
    (dext-util/print-fs-action "ln -s" src dest meta)
    (dfs-util/lns src dest)))

(deftype dlns-ext [] duffel-extension
    (preprocess-file [x file-tree] file-tree)
    (preprocess-dir [x dir-tree] dir-tree)

    (file-meta-tpl [x] {})
    (dir-meta-tpl [x] {})

    (postprocess-file [x file-struct] file-struct)

    (postprocess-dir [x dir-struct]
      ;; Once we ln -s a dir we don't want to do anything to it's children
      [(first dir-struct)])

    (process-file [x app meta-struct abs local]
        (when-not (dfs-util/exists? abs)
          (ln-file local abs meta-struct)))

    (process-dir [x app meta-struct abs local]
        (when-not (dfs-util/exists? abs)
          (ln-file local abs meta-struct)))
)

(dext/register-ext "dlns" (->dlns-ext))
