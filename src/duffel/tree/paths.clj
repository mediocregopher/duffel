(ns duffel.tree.paths
  "Functions for filling out duffel trees with necessary information and pruning
  them of unecessary dirs/files/information"
  (:require [duffel.tree.core :refer [tree-map tree-contents-map
                                      tree-get tree-assoc]]))

(defn- try-append-slash
  "Given a string representing a directory, appends a slash to the string if
  it's not already there"
  [dir]
  (if-not (= (last dir) \/) (str dir "/") dir))

(defn- set-path
  [el path-type name-key root]
  (if (sequential? el)
    (tree-assoc el path-type (str root (tree-get el name-key) "/"))
    (tree-assoc el path-type (str root (tree-get el name-key)))))

(defn fill-relpaths
  "Goes through a duffel tree and fills all relative paths for both dir-maps and
  file-maps"
  [app dtree]
  (tree-map
    (fn [dt]
      (tree-contents-map
        #(set-path % :rel-path :orig-name (tree-get dt :rel-path))
        dt))
    (-> dtree
      (set-path :rel-path :orig-name (app :proj-root))
      (tree-assoc :real-name "/"))))

(defn fill-abspaths
  "Goes through a duffel tree and fills all absolute paths for both dir-maps and
  file-maps. The absolute paths are chrooted such that the root of the tree's
  abs-path is the given chroot, and all children follow from that"
  [app dtree]
  (tree-map
    (fn [dt]
      (tree-contents-map
        #(set-path % :abs-path :real-name (tree-get dt :abs-path))
        dt))
    (tree-assoc dtree :abs-path (try-append-slash (app :chroot)))))

(defn fill-paths
  "Goes through a duffel tree and fills all rel and abs paths using
  fill-relpaths and fill-abspaths. root is passed through to fill-abspaths"
  [app dtree]
  (->> dtree
    (fill-abspaths app)
    (fill-relpaths app)))

(comment
  (require '[duffel.tree.core :refer :all])
  (require '[clojure.pprint :refer [pprint]])

  (pprint
    (fill-relpaths (dir->tree "my-duffel")))

  (pprint
    (fill-paths {:chroot "/tmp/duffel-test"} (dir->tree "my-duffel")))
)
