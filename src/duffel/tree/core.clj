(ns duffel.tree.core
  "Core functionality for creating and working with duffel trees"
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

(defn tree-map
  "Given a duffel tree, run f on that duffel tree. Look through the returned
  duffel tree for more duffel trees, and call f on them. Recurse."
  [f dtree]
  (let [new-dtree (f dtree)]
    (cons
      (first new-dtree)
      (map #(if (sequential? %) (tree-map f %) %) (rest new-dtree)))))

(defn tree-contents-map
  "Same as tree-map, but doesn't descend down into sub-directories and only runs
  over the contents of the given duffel tree, not the head itself"
  [f dtree]
  (cons (first dtree)
    (map f (rest dtree))))

(defn tree-get
  "Given some item from a duffel tree, either a file or another tree, returns
  the value of a key in it"
  [el k & default]
  (let [d (first default)]
    (if (sequential? el) ((first el) k d) (el k d))))

(defn tree-assoc
  "Given some item from a duffel tree, either a file or another tree, associates
  the given key/value pairs in its map"
  [el & kvs]
  (if (sequential? el)
    (cons (apply assoc (first el) kvs) (rest el))
    (apply assoc el kvs)))
