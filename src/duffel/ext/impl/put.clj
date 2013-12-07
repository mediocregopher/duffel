(ns duffel.ext.impl.put
  (:require [duffel.ext.core :refer [duffel-extension add-impl]]
            [duffel.ext.util  :refer :all]
            [duffel.tree.core :refer [tree-contents-map]]
            [duffel.fs        :refer [exists? mkdir-p ls rm-rf cp]]))

(deftype put-ext [] duffel-extension

  (process-dir [x app dtree]
    (let [local (meta-get dtree :rel-path)
          abs   (meta-get dtree :abs-path)
          [owner group chmod] (dir-filled-perms dtree)]

      (when-not (perm-same? abs owner group chmod)
        (print-fs-action "mkdir" local abs owner group chmod)
        (mkdir-p abs)
        (force abs owner group chmod))

      (when (meta-get dtree "delete-untracked")
        (let [present   (set (ls abs))
              tracked   (tree-contents-map #(% :real-name) dtree)
              untracked (apply disj present tracked)]
          (doseq [filedir untracked]
            (let [full-path (str abs filedir)]
              (println "rm" full-path)
              (try-backup full-path)
              (rm-rf full-path))))))
    dtree)

  (process-file [x app file-map]
    (let [local (meta-get file-map :rel-path)
          abs   (meta-get file-map :abs-path)
          [owner group chmod] (file-filled-perms file-map)]
      (when-not (files-same? local abs owner group chmod)
          (try-backup app abs)
          (print-fs-action "cp" local abs owner group chmod)
          (cp local abs)
          (force-perms abs owner group chmod))))

)

(add-impl "put" (->put-ext))
