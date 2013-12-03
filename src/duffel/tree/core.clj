(ns duffel.tree.core
  "Core functionality for creating and working with duffel trees"
  (:require [clojure.walk :refer [walk]])
  (:import java.io.File))

(def dot-underscore-split #"(.+)\._(.+)$")
(def comma-split #"([^,]*),?(.*)$")

(def default-ext "put")
(def default-spec "_default")

(defn split-file-name
  "Splits a given file-name into the real file-name and the duffel information
  that could be trailing after a \"._\". Returns a vector of the real name and
  the trailing info, both as strings. Trailing info could be an empty string if
  it's not found"
  [file-name]
  (if-let [[_ real-name trailing] (re-find dot-underscore-split file-name)]
    [real-name trailing]
    [file-name ""]))

(defn split-ext-spec
  "Given the section of a file after the \"._\", splits up the extension and
  host specifier based on the comma position, fills in default values if nothing
  is given for either of the values. Returns the data as a vector of [ext spec]"
  [trailing]
  (if-let [[_ ext spec] (re-find comma-split trailing)]
    [ (if (empty? ext) default-ext ext)
      (if (empty? spec) default-spec spec) ]
    [ default-ext
      default-spec ]))

(defn explode-file
  "Given a file-name, explodes it into its potential parts, both the real
  file-name and the duffel info that could be embedded in it. Default values for
  the duffel info are put in place if they're not found"
  [file-name]
  (let [[real-name trailing] (split-file-name file-name)
        [ext spec]           (split-ext-spec trailing)]
    { :real-name real-name
      :extension ext
      :specifier spec
      :orig-name file-name }))

(defn create-dir-tree
  "Given a directory name, turns it into a flat tree. A flat tree is simply a
  list which contains either strings or flat trees"
  [dir]
  (let [fo (if (instance? File dir) dir (File. dir))
        fo-ls (.listFiles fo)]
    (cons (.getName fo)
      (map #(if (.isDirectory %)
        (create-dir-tree %) (.getName %)) fo-ls))))

(defn explode-tree
  "Given a flat tree, explodes all elements of it"
  [tree]
  (walk
    #(if (seq? %) (explode-tree %) (explode-file %))
    identity
    tree))

(defn dir->tree
  "Given a directory name, turns it into a duffel tree"
  [dir]
  (-> dir create-dir-tree explode-tree))

(defn tree-dir-mapreduce
  "Given a duffel tree, runs f on each dir map (the first item in each list).
  The first argument to f is the dir map itself. The second is an accumulator.
  The third is the list of file maps for that dir map. f returns a vector of the
  new dir map and new accumulator which will be given to all child calls from
  that dir. If the new dir map is nil then that dir is removed and its children
  are not processed. The file map list is unaffected"
  [f acc dtree]
  (let [dir-map (first dtree)
        file-maps (rest dtree)
        [new-dir-map new-acc] (f dir-map acc file-maps)]
    (when-not (nil? new-dir-map)
      (cons new-dir-map
        (remove nil?
          (map
            #(if (sequential? %) (tree-dir-mapreduce f new-acc %) %)
            file-maps))))))

(defn tree-file-map
  "Given a duffel tree, runs f on each file map (any map that's not the first
  item in a list). The first argument to f is the dir map for the file, the
  second is the file map currently being looked at. f returns the new file map
  to be put in place. If f returns nil then the file map will be ommitted from
  the returned tree"
  [f dtree]
  (let [dir-map (first dtree)
        file-maps (rest dtree)]
    (cons dir-map
      (remove nil?
        (map
          #(if (sequential? %) (tree-file-map f %) (f dir-map %))
          file-maps)))))

(defn tree-map
  "Given a duffel tree, run f on that duffel tree. Look through the returned
  duffel tree for more duffel trees, and call f on them. Recurse."
  [f dtree]
  (let [new-dtree (f dtree)]
    (cons
      (first new-dtree)
      (map #(if (sequential? %) (tree-map f %) %) (rest new-dtree)))))

(defn dir-get
  "Get a key from the dir map at the root of the given tree"
  [dtree k]
  (k (first dtree)))

(defn dir-assoc
  "Does an assoc on the dir map at the root of the given tree"
  [dtree & kvs]
  (cons
    (apply assoc (first dtree) kvs)
    (rest dtree)))

(defn file-map
  "Calls f on each file (skipping child directories) in the root directory of
  the given tree. Returns sequence of returned values"
  [f dtree]
  (map f (remove sequential? (rest dtree))))

(comment
  (require '[clojure.pprint :refer [pprint]])

  (pprint
    (tree-dir-mapreduce
      (fn [dir acc files]
        (if (= (dir :real-name) "wat1") [nil acc]
          [(assoc dir :len-files (count files) :path acc)
           (str acc "/" (dir :real-name))]))
      ""
      (dir->tree "my-duffel")))

  (pprint
    (tree-file-map
      (fn [dir file]
        (when-not (= (file :real-name) "b.txt")
          (assoc file :dir-name (dir :real-name))))
      (dir->tree "my-duffel")))

  (dir-get (dir->tree "my-duffel") :real-name)

  (dir-assoc (dir->tree "my-duffel") :a "a" :b "b")
)
