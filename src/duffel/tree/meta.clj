(ns duffel.tree.meta
  "meta provides functions for reading and folding in metadata files in the
  duffel tree"
  (:require [duffel.tree.core :refer [tree-map tree-get tree-assoc
                                      tree-contents-map]]
            [cheshire.core :refer [parse-string]]
            [clojure.string :as s]))

(defn rname-matches?
  "Given a file-map or another duffel tree, returns whether or not its real-name
  matches the given file-blob"
  [el fileglob]
  (let [regex-str (s/replace fileglob "*" ".*")
        regex (re-pattern (str "^" regex-str "$"))]
    (not (nil? (re-find regex (tree-get el :real-name))))))

(defn meta-file?
  "Returns whether or not the given file-map is a metadata file (returns false
  always if given a directory"
  [el]
  (and (not (sequential? el))
    (= "_meta.json" (el :real-name))))

(defn pluck-meta
  "Given a list of file-maps or duffel trees, returns a vector where the first
  item is a file-map for the metadata file (or nil if not found) and the second
  the same list with that item removed"
  [el-seq]
  (let [[m r]
    (reduce
      (fn [[meta els] el]
        (if (meta-file? el) [el els]
          [meta (cons el els)]))
      [nil '()]
      el-seq)]
    [m (reverse r)]))

(defn read-meta-file
  "Given a file-map, returns its contents parsed into a map with keywords as
  keys. Throws an exception if reading or parsing fails. Assumes rel-path has
  already been filled in on the file-map"
  [file-map]
  (parse-string (slurp (file-map :rel-path))))

(defn meta-onto-file
  "Given a file-map or a duffel tree, tries to find a key in the given meta map
  that matches its real-name, and assocs the metadata at that key to :meta on
  the file-map/duffel tree, returning it. Returns it unchanged if no match, and
  stops after the first match"
  [el meta]
  (reduce
    (fn [_ [glob el-meta]]
      (if (rname-matches? el glob)
        (reduced (tree-assoc el :meta el-meta))
        el))
    el
    meta))

(defn meta-onto-branch
  "Given a duffel tree and a metadata object, goes through every item in the
  tree and attempts to handle putting the correct metadata on it. Also handles
  the \".\" case"
  [dtree meta]
  (let [meta-filtered (dissoc meta ".")
        el-seq-rest (map #(meta-onto-file % meta-filtered) (rest dtree))
        el-root (if-let [m (meta ".")]
                  (assoc (first dtree) :meta m)
                  (meta-onto-file (first dtree) meta))]
    (cons el-root el-seq-rest)))

(defn collapse-meta
  "Given a duffel tree, goes through and plucks off all meta-data files and
  disperses their contents to the appropriate destinations"
  [dtree]
  (tree-map
    (fn [dt]
      (let [[meta-file dt-rest] (pluck-meta dt)]
        (if (nil? meta-file) dt
          (meta-onto-files dt-rest
            (read-meta-file meta-file)))))
    dtree))

(defn cascade-meta
  "Given a duffel tree, goes through and finds all instances of
  \"apply_shallow\" or \"apply_deep\" and appropriately cascades them down the
  tree"
  [dtree]
  (tree-map
    (fn [dt]
      (let [root-meta (tree-get dt :meta {})
            clean-root-meta (dissoc root-meta "apply_shallow")]
        (if (or (root-meta "apply_shallow") (root-meta "apply_deep"))
          (tree-contents-map
            #(let [curr-meta (tree-get % :meta {})
                   new-meta  (merge clean-root-meta curr-meta)]
              (tree-assoc % :meta new-meta))
            dt)
          dt)))
    dtree))

(comment
  (require '[duffel.tree.core :refer :all])
  (require '[duffel.tree.paths :refer :all])
  (require '[clojure.pprint :refer [pprint]])

  (pprint
    (cascade-meta
      (collapse-meta
        (fill-relpaths (dir->tree "my-duffel")))))

)
