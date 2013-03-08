(ns duffel-ext.put
    (:use duffel.ext-protocol)
    (:require [duffel.fs-util  :as dfs-util]
              [duffel.ext      :as dext]
              [duffel.ext-util :as dext-util]))

(deftype touch-ext [] duffel-extension
    (preprocess-file [x file-tree] file-tree)
    (preprocess-dir [x dir-tree] dir-tree)

    (file-meta-tpl [x] dext-util/file-ownership-tpl)
    (dir-meta-tpl [x] {})

    (postprocess-file [x file-struct] file-struct)
    (postprocess-dir [x dir-struct] dir-struct)

    (process-file [x app meta-struct abs local]
        (dext-util/print-fs-action "touch" local abs meta-struct)
        (dfs-util/touch abs)
        (dext-util/try-ownership abs meta-struct))

    (process-dir [x app meta-struct abs local]
        (throw (Exception. "touch extension doesn't support handling directories")))
)

(dext/register-ext "touch" (->touch-ext))
