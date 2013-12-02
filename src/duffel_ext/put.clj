(ns duffel-ext.put
    (:use duffel.ext-protocol)
    (:require [duffel.fs-util  :as dfs-util]
              [duffel.util     :as dutil]
              [duffel.ext      :as dext]
              [duffel.backup   :as dbackup]
              [duffel.ext-util :as dext-util]))

(defn clean-meta-struct
    "We don't want these to ever propogate, so we can use this function to clean
    them out"
    [meta-struct]
    (-> meta-struct (dissoc :apply_to_contents)
                    (dissoc :apply_recursively)))

(defn meta->dir-tree
    "Applies a meta-struct (reverse) to every item in a dir-tree"
    [dir-tree meta-struct]
    (map #(if (seq? %) (dfs-util/merge-meta-dir-reverse % meta-struct)
                       (dfs-util/merge-meta-reverse % meta-struct)) dir-tree))

(defmulti _preprocess-dir
    (fn [dir-tree]
        (let [root-meta ((first dir-tree) :meta {})]
            (cond (root-meta :apply_recursively) :apply_recursively
                  (root-meta :apply_to_contents) :apply_to_contents
                  :else                          :apply_only_here   ))))

(defmethod _preprocess-dir :apply_only_here [dir-tree] dir-tree)

(defmethod _preprocess-dir :apply_to_contents [dir-tree]
    (let [ dir-tree-root (first dir-tree)
           meta-struct   (clean-meta-struct (dir-tree-root :meta {})) ]
        (cons dir-tree-root (meta->dir-tree (rest dir-tree) meta-struct))))

(defmethod _preprocess-dir :apply_recursively [dir-tree]
    (let [ dir-tree-root (first dir-tree)
           meta-struct   (clean-meta-struct (dir-tree-root :meta {})) ]
    (cons dir-tree-root
        (rest (dfs-util/tree-map
                  (fn [d _ _] (meta->dir-tree d meta-struct))
                  dir-tree)))))

(deftype put-ext [] duffel-extension

    ;This one needed special attention, since a defmulti was the most efficient
    ;way to implement it
    (preprocess-dir [x dir-tree] (_preprocess-dir dir-tree))

    (file-meta-tpl [x] dext-util/file-ownership-tpl)

    (dir-meta-tpl [x]
        (merge dext-util/dir-ownership-tpl
           { :delete_untracked  '(:bool (:optional false))
             :force_ownership   '(:bool (:optional false)) }))

    ;These just return what they're given, no changed to the structs in these
    ;stages
    (preprocess-file [x file-struct] file-struct)
    (postprocess-file [x file-struct] file-struct)

    ;We go through after templating and attach a list of tracked items to each
    ;directory. This is needed for :delete_untracked
    (postprocess-dir [x dir-struct]
        (let [tracked (->> (rest dir-struct)
                           (map
                              #(if (seq? %)
                                ((first %) :base-name)
                                (% :base-name))))]
            (dfs-util/merge-meta-dir dir-struct {:tracked tracked})))

    (process-dir [x app meta-struct abs local]
        (when-not (dext-util/dirs-no-difference? local abs meta-struct)
            (dext-util/print-fs-action "mkdir" local abs meta-struct)
            (dfs-util/mkdir-p abs)
            (dext-util/try-ownership abs meta-struct))

        (when (meta-struct :delete_untracked)
            (let [ tracked-files   (meta-struct :tracked)
                   present-files   (set (dfs-util/ls abs))
                   untracked       (apply disj
                                      (cons present-files tracked-files)) ]
                (doseq [filedir untracked]
                    (let [full-path (str abs "/" filedir)]
                        (println "rm" full-path)
                        (dfs-util/rm-rf full-path))))))


    (process-file [x app meta-struct abs local]
        (when-not (dext-util/files-no-difference? local abs meta-struct)
            (dext-util/try-backup app abs)
            (dext-util/print-fs-action "cp" local abs meta-struct)
            (dfs-util/cp local abs)
            (dext-util/force-ownership abs meta-struct)))

)

(dext/register-ext "put" (->put-ext))
