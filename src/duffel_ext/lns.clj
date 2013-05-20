(ns duffel-ext.lns
    (:use duffel.ext-protocol)
    (:require [duffel.fs-util  :as dfs-util]
              [duffel.ext      :as dext]
              [duffel.ext-util :as dext-util]))

(deftype lns-ext [] duffel-extension
    (preprocess-file [x file-tree] file-tree)
    (preprocess-dir [x dir-tree] dir-tree)

    (file-meta-tpl [x] dext-util/file-ownership-tpl)
    (dir-meta-tpl [x] {})

    (postprocess-file [x file-struct] file-struct)

    (postprocess-dir [x dir-struct] 
      ;; Once we ln -s a dir we don't want to do anything to it's children
      [(first dir-struct)])
  

    (process-file [x app meta-struct abs local]
        (when-not (dfs-util/exists? abs)
            (dext-util/print-fs-action "ln -s" local abs meta-struct)
            (dfs-util/lns local abs)))

    (process-dir [x app meta-struct abs local]
        (when-not (dfs-util/exists? abs)
            (dext-util/print-fs-action "ln -s" local abs meta-struct)
            (dfs-util/lns local abs)))
)

(dext/register-ext "lns" (->lns-ext))
