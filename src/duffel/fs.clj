(ns duffel.fs
    (:require [duffel.ext     :as dext]
              [duffel.meta    :as dmeta]
              [duffel.util    :as dutil]
              [duffel.fs-util :as dfs-util]
              [duffel.translation :as dtran]
              [duffel.script  :as dscript]
              [clojure.walk :refer [walk]])
    (:import java.io.File))


(defn translate-dir-tree
    "Given a dir-tree, attempts to translate the root node's base-name"
    [dir-tree]
    (let [dir-struct (first dir-tree)
          dir-struct-translated (assoc dir-struct
                                  :base-name (dtran/translate-dir
                                                (dir-struct :base-name)))]
        (cons dir-struct-translated (rest dir-tree))))

(defn translate-top-level
    "Given a dir-tree, goes through all top level directories and runs
    translate-dir-tree on them"
    [dir-tree]
    (cons (first dir-tree)
      (map #(if (seq? %) (translate-dir-tree %) %) (rest dir-tree))))

(defn remove-special-dirs
    [dir-tree]
    (cons (first dir-tree)
          (remove
            #(and (seq? %) (dtran/is-special? ((first %) :base-name)))
            (rest dir-tree))))

(defn filter-git
    "Function to be passed into tree-map which will remove all directories
    called .git"
    [dir-tree _ _]
    (remove #(and (seq? %) (= ((first %) :base-name) ".git")) dir-tree))
