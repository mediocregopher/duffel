(ns duffel.tree.paths
  "Functions for filling out duffel trees with necessary information and pruning
  them of unecessary dirs/files/information"
  (:require [duffel.tree.core :refer [tree-dir-mapreduce
                                      tree-file-map
                                      tree-assoc]]))

(defn- try-append-slash
  "Given a string representing a directory, appends a slash to the string if
  it's not already there"
  [dir]
  (if-not (= (last dir) \/) (str dir "/") dir))

(defn fill-relpaths
  "Goes through a duffel tree and fills all relative paths for both dir-maps and
  file-maps"
  [dtree]
  (->> dtree
    (tree-dir-mapreduce
      (fn [dir-map acc _]
        (let [rel-path (str acc (dir-map :real-name) "/")]
          [ (assoc dir-map :rel-path rel-path)
            rel-path ]))
      "")
    (tree-file-map
      (fn [dir-map file-map]
        (assoc file-map
          :rel-path (str (dir-map :rel-path) (file-map :real-name)))))))

(defn fill-abspaths
  "Goes through a duffel tree and fills all absolute paths for both dir-maps and
  file-maps. The absolute paths are chrooted such that the root of the tree's
  abs-path is the given chroot, and all children follow from that"
  [dtree root]
  (let [real-root (try-append-slash root)
        rooted-dtree (tree-assoc dtree :abs-path real-root)]
    (->> rooted-dtree
      (tree-dir-mapreduce
        (fn [dir-map acc _]
          (let [new-root (or (dir-map :abs-path)
                             (str acc (dir-map :real-name) "/"))]
            [ (assoc dir-map :abs-path new-root) new-root ]))
        "")
      (tree-file-map
        (fn [dir-map file-map]
          (assoc file-map
            :abs-path (str (dir-map :abs-path) (file-map :real-name))))))))

(defn fill-paths
  "Goes through a duffel tree and fills all rel and abs paths using
  fill-relpaths and fill-abspaths. root is passed through to fill-abspaths"
  [dtree root]
  (-> dtree
    (fill-abspaths root)
    (fill-relpaths)))

(comment
  (require '[duffel.tree.core :refer :all])
  (require '[clojure.pprint :refer [pprint]])

  (pprint
    (fill-relpaths (dir->tree "my-duffel")))

  (pprint
    (fill-paths (dir->tree "my-duffel") "/tmp/duffel-test"))
)
