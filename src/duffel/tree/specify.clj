(ns duffel.tree.specify
  "specify provides functions for going through a duffel tree and narrowing down
  files with the same name but different hostname/groupname specifiers so we're
  only left with the correct ones"
  (:require [duffel.tree.core :refer [tree-map]]
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

(defn- df-rname [df]
  (if (sequential? df) (:real-name (first df)) (:real-name df)))

(defn- df-spec [df]
  (if (sequential? df) (:specifier (first df)) (:specifier df)))

(defn specified-entry
  "Given a sequence containing dir-maps or file-maps, returns the most specific
  one for this node, or nil if none of them match"
  [proj-root dirfile-maps]
  (thread-until dirfile-maps
    (partial matches-when #(fdqn-matches                (df-spec %)))
    (partial matches-when #(hostname-matches            (df-spec %)))
    (partial matches-when #(groupname-matches proj-root (df-spec %)))
    (partial matches-when #(= "_default"                (df-spec %)))))

(defn narrow-dirfiles
  "Given a sequence containing dir-maps or file-maps, groups them all together
  based on their real names and narrows the groups down to the proper one, or
  removes if none in the group are proper"
  [proj-root dirfile-maps]
  (->> dirfile-maps
    (reduce
      #(let [rname (df-rname %2)
             rname-seq (%1 rname '())]
        (assoc %1 rname (cons %2 rname-seq)))
      {})
    (reduce
      (fn [ret [rname rname-seq]]
        (if-let [specd (specified-entry proj-root rname-seq)]
          (cons specd ret)
          ret))
      '())))

(defn specify-tree
  "Given a duffel tree, goes through all of its branches, grouping together
  files with the same real names and keeping the ones with the most specific
  specifiers"
  [proj-root dtree]
  (tree-map
    #(cons (first %) (narrow-dirfiles proj-root (rest %)))
    dtree))


(comment
  (require '[clojure.pprint :refer [pprint]])
  (require '[duffel.tree.core :refer [dir->tree]])

  (pprint
    (specify-tree "my-duffel" (dir->tree "my-duffel")))
)
