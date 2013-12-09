(ns duffel.ext.impl.clerb
  (:require [duffel.ext.core :refer [duffel-extension add-impl]]
            [duffel.ext.util :refer :all]
            [duffel.fs       :refer [exists?]]
            [clerb.core      :refer [clerb-file]]))

(deftype clerb-ext [] duffel-extension

  (process-dir [x app dtree]
    (throw
      (Exception. "clerb extension doesn't support handling directories")))

  (process-file [x app file-map]
    (let [local (file-map :rel-path)
          abs   (file-map :abs-path)
          [owner group chmod] (file-filled-perms file-map)
          tpl-parsed (clerb-file local)]
      (when-not (and (perm-same? abs owner group chmod)
                     (= tpl-parsed (slurp abs)))
        (try-backup app abs)
        (print-fs-action "clerb" local abs owner group chmod)
        (spit abs tpl-parsed)
        (force-perms abs owner group chmod))))
)

(add-impl "clerb" (->clerb-ext))
