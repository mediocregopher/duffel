(ns duffel.tree.specify
  "specify provides functions for going through a duffel tree and narrowing down
  files with the same name but different hostname/groupname specifiers so we're
  only left with the correct ones"
  (:require [duffel.tree.core :refer [tree-dir-mapreduce
                                      tree-file-map
                                      dir-assoc]]

            [duffel.script    :refer [is-in-group?]]))

(def my-fdqn (.getCanonicalHostName (java.net.InetAddress/getLocalHost)))
(defn fdqn-matches [hostname]
  (= my-fdqn hostname))

(def my-hostname (.getHostName (java.net.InetAddress/getLocalHost)))
(defn hostname-matches [hostname]
  (= my-hostname hostname))

(defn groupname-matches [proj-root groupname]
  (is-in-group? proj-root groupname))

(defn- thread-until [init & fns]
  "Runs each fn on init until one of them doesn't return nil, and returns that
  result. Returns nil if none of them return anything but nil"
  (reduce
    (fn [_ f]
      (let [r (f init)]
        (if (nil? r) nil (reduced r))))
    nil
    fns))

(defn- matches-when
  "Runs pred on each item until it returns true, returning that item. Returns
  nil if no items returned true"
  [pred items]
  (reduce
    (fn [_ item]
      (if (pred item) (reduced item) nil))
    nil
    items))

(defn specified-entry
  "Given a sequence of file-maps, returns the most specific one for this node,
  or nil if none of them match"
  [proj-root file-maps]
  (thread-until file-maps
    (partial matches-when #(fdqn-matches                (:specifier %)))
    (partial matches-when #(hostname-matches            (:specifier %)))
    (partial matches-when #(groupname-matches proj-root (:specifier %)))
    (partial matches-when #(= "_default"                (:specifier %)))))

(defn add-to-basename-group
    "Given a map and a file struct, creates a list with the file-struct as the
    only item for the index of the map corresponding to the file- struct's
    :base-name. If the list already exists in the given map, append to it
    instead"
    [file-map file-struct]
    (let [base-name (file-struct :base-name)
          base-name-seq (file-map base-name '())
          new-seq (cons file-struct base-name-seq)]
        (assoc file-map base-name new-seq)))

(defn narrow-group
    "Given a list of file-maps, returns the map with the most specific
    specifier"
    [proj-root group-seq]
    (let [specifiers (map #(% :specifier) group-seq)]
        (when-let [i (index-of-specified-entry proj-root specifiers)]
            (nth group-seq i))))
