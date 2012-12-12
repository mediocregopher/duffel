(ns duffel-ext.put
    (:require [duffel.fs-util :as dfs-util])
    (:import java.lang.System))

(def default-username (java.lang.System/getProperty "user.name"))
(def default-group default-username)    ;Assume primary group = username, cause fuck it

(defn file-meta-tpl []
    { :chmod (list :string (list :optional "0644") '(:regex #"^[0-7]{3,4}$"))
      :owner (list :string (list :optional default-username)             )
      :group (list :string (list :optional default-group)                ) })

(defn dir-meta-tpl []
    (merge (file-meta-tpl)
       { :chmod (list :string (list :optional "0755") '(:regex #"^[0-7]{3,4}$")) 
         :delete_untracked  '(:bool (:optional false)) }))

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


(defmulti preprocess-dir
    (fn [dir-tree] 
        (let [root-meta ((first dir-tree) :meta {})]
            (cond (root-meta :apply_recursively) :apply_recursively
                  (root-meta :apply_to_contents) :apply_to_contents
                  :else                          :apply_only_here   ))))

(defmethod preprocess-dir :apply_only_here [dir-tree] dir-tree)

(defmethod preprocess-dir :apply_to_contents [dir-tree]
    (let [ dir-tree-root (first dir-tree)
           meta-struct   (clean-meta-struct (dir-tree-root :meta {})) ]
        (cons dir-tree-root (meta->dir-tree (rest dir-tree) meta-struct))))

(defmethod preprocess-dir :apply_recursively [dir-tree]
    (let [ dir-tree-root (first dir-tree)
           meta-struct   (clean-meta-struct (dir-tree-root :meta {})) ]
    (cons dir-tree-root
        (dfs-util/tree-map 
            (fn [d _ _] (meta->dir-tree d meta-struct)) 
            (rest dir-tree)))))

(defn preprocess-file  [file-struct] file-struct)
(defn postprocess-dir  [dir-struct]  dir-struct)
(defn postprocess-file [file-struct] file-struct)

(defn process-file [meta-struct abs local]
    (println "cp" local "->" abs "::" 
        (meta-struct :chmod) (str (meta-struct :owner) ":" (meta-struct :group)))
    (dfs-util/cp local abs)
    (dfs-util/chown (meta-struct :owner) (meta-struct :group) abs)
    (dfs-util/chmod (meta-struct :chmod) abs))

(defn process-dir [meta-struct abs local]
    (println "mkdir" local "->" abs "::" 
        (meta-struct :chmod) (str (meta-struct :owner) ":" (meta-struct :group)))
    (dfs-util/mkdir-p abs)
    (dfs-util/chown (meta-struct :owner) (meta-struct :group) abs)
    (dfs-util/chmod (meta-struct :chmod) abs))