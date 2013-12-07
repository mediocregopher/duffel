(ns duffel.ext.impl.clerb
    (:use clerb.core)
    (:require [duffel.ext.core :refer [duffel-extension]]
              [duffel.ext.util :refer :all]
              [duffel.fs       :refer [exists?]]))

(deftype clerb-ext [] duffel-extension

  (process-dir [x app dtree]
    (throw
      (Exception. "clerb extension doesn't support handling directories")))

  (process-file [x app file-map]
    (let [local (file-map :rel-path)
          abs   (file-map :abs-path)
          [owner group chmod] (file-filled-perms file-map)
          tpl-parsed (clerb-file local)]
      (when-not (and (dfs/exists? abs)
                     (= tpl-parsed (slurp abs))
                     (perm-same? abs owner group chmod))
        (try-backup app abs)
        (print-fs-action "clerb" local abs owner group chmod)
        (spit abs (clerb-file local))
        (force-perms abs owner group chmod))))
)
