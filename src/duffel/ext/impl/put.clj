(ns duffel.ext.impl.put
  (:require [duffel.ext.core :refer [duffel-extension add-impl]]
            [duffel.ext.util  :refer :all]
            [duffel.tree.core :refer [tree-get tree-contents-map]]
            [duffel.fs        :refer [exists? mkdir-p ls rm-rf cp]]))

(deftype put-ext [] duffel-extension

  (process-dir [x app dtree]
    (let [local (tree-get dtree :rel-path)
          abs   (tree-get dtree :abs-path)
          [owner group chmod] (dir-filled-perms dtree)]

      (when-not (perm-same? abs owner group chmod)
        (print-fs-action "mkdir" local abs owner group chmod)
        (mkdir-p abs)
        (force-perms abs owner group chmod))

      (when (meta-get dtree "delete_untracked")
        (let [present   (set (ls abs))
              tracked   (rest (tree-contents-map #(tree-get % :real-name) dtree))
              untracked (apply disj present tracked)]
          (doseq [filedir untracked]
            (let [full-path (str abs filedir)]
              (println "rm" full-path)
              (try-backup app full-path)
              (rm-rf full-path))))))
    dtree)

  (process-file [x app file-map]
    (let [local (tree-get file-map :rel-path)
          abs   (tree-get file-map :abs-path)
          [owner group chmod] (file-filled-perms file-map)]
      (when-not (files-same? local abs owner group chmod)
          (try-backup app abs)
          (print-fs-action "cp" local abs owner group chmod)
          (cp local abs)
          (force-perms abs owner group chmod))))

)

(add-impl "put" (->put-ext))
