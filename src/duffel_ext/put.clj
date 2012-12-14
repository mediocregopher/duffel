(ns duffel-ext.put
    (:use duffel.ext-protocol)
    (:require [duffel.fs-util :as dfs-util]
              [duffel.ext     :as dext])
    (:import java.lang.System))

(def default-username (java.lang.System/getProperty "user.name"))
(def default-group default-username)    ;Assume primary group = username, cause fuck it

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
        (dfs-util/tree-map 
            (fn [d _ _] (meta->dir-tree d meta-struct)) 
            (rest dir-tree)))))

(deftype put-ext [] duffel-extension

    ;This one needed special attention, since a defmulti was the most efficient way to
    ;implement it
    (preprocess-dir [x dir-tree] (_preprocess-dir dir-tree))

    (file-meta-tpl [x]
        { :chmod (list :string (list :optional "0644") '(:regex #"^[0-7]{3,4}$"))
          :owner (list :string (list :optional default-username)             )
          :group (list :string (list :optional default-group)                ) })

    (dir-meta-tpl [x]
        (merge (file-meta-tpl x)
           { :chmod (list :string (list :optional "0755") '(:regex #"^[0-7]{3,4}$")) 
             :delete_untracked  '(:bool (:optional false)) }))

    ;These just return what they're given, no changed to the structs in these stages
    (preprocess-file [x file-struct] file-struct)
    (postprocess-file [x file-struct] file-struct)

    (postprocess-dir [x dir-struct]
        (let [file-list (->> (rest dir-struct)
                             (remove seq?)
                             (map #(% :base-name)))]
            (dfs-util/merge-meta-dir dir-struct {:file-list file-list})))

    (process-dir [x meta-struct abs local]
        (println "mkdir" local "->" abs "::" 
            (meta-struct :chmod) (str (meta-struct :owner) ":" (meta-struct :group)))
        (dfs-util/mkdir-p abs)
        (dfs-util/chown (meta-struct :owner) (meta-struct :group) abs)
        (dfs-util/chmod (meta-struct :chmod) abs))


    (process-file [x meta-struct abs local]
        (println "cp" local "->" abs "::" 
            (meta-struct :chmod) (str (meta-struct :owner) ":" (meta-struct :group)))
        (dfs-util/cp local abs)
        (dfs-util/chown (meta-struct :owner) (meta-struct :group) abs)
        (dfs-util/chmod (meta-struct :chmod) abs))

)

(dext/register-ext "put" (->put-ext))
