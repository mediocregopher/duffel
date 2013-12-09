(ns duffel.ext.impl.touch
  (:require [duffel.ext.core  :refer [duffel-extension add-impl]]
            [duffel.ext.util  :refer :all]
            [duffel.tree.core :refer [tree-get]]
            [duffel.fs        :refer [touch]]))

(deftype touch-ext [] duffel-extension

  (process-dir [x app dtree]
    (throw
      (Exception. "touch extension doesn't support handling directories")))

  (process-file [x app file-map]
    (let [local (tree-get file-map :rel-path)
          abs   (tree-get file-map :abs-path)
          [owner group chmod] (file-filled-perms file-map)]
      (when-not (perm-same? abs owner group chmod)
        (print-fs-action "touch" local abs owner group chmod)
        (touch abs)
        (force-perms abs owner group chmod))))
)

(add-impl "touch" (->touch-ext))
