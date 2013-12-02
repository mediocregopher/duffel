(ns duffel-ext.clerb
    (:use duffel.ext-protocol)
    (:use clerb.core)
    (:require [duffel.fs-util  :as dfs-util]
              [duffel.util     :as dutil]
              [duffel.ext      :as dext]
              [duffel.backup   :as dbackup]
              [duffel.ext-util :as dext-util]))

(deftype clerb-ext [] duffel-extension

    (preprocess-dir  [x dir-tree]    dir-tree)
    (preprocess-file [x file-struct] file-struct)

    (dir-meta-tpl  [x] {})
    (file-meta-tpl [x] dext-util/file-ownership-tpl)

    (postprocess-dir  [x dir-struct]  dir-struct)
    (postprocess-file [x file-struct] file-struct)

    (process-dir [x app meta-struct abs local]
        (throw
          (Exception. "clerb extension doesn't support handling directories")))

    (process-file [x app meta-struct abs local]
        (let [tpl-parsed (clerb-file local)]
            (when-not (and (dfs-util/exists? abs)
                           (= tpl-parsed (slurp abs))
                           (dext-util/perm-same? abs meta-struct))
                (dext-util/try-backup app abs)
                (dext-util/print-fs-action "clerb" local abs meta-struct)
                (spit abs (clerb-file local))
                (dext-util/force-ownership abs meta-struct))))
)

(dext/register-ext "clerb" (->clerb-ext))
