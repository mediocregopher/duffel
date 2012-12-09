(ns duffel.fs
    (:require [duffel.ext  :as ext]
              [duffel.util :as util])
    (:import java.io.File))


(defn _tree [fo]
    (let [fo-ls (.listFiles fo)]
        (cons (.getName fo) 
            (->> (filter #(not (.isHidden %)) fo-ls)
                 (map #(if (.isDirectory %) (_tree %) (.getName %)))))))

(defn tree [dir]
    (_tree (File. dir)))

(def dot-underscore-split #"(.+)\._(.+)$")

(defn host-specifier-split [file-name]
    "Given a file name, returns a list where first item is
    the base filename (possibly with duffel extension), and the
    second is the host specifier (or '_default')"
    (if-let [re-res (re-find dot-underscore-split file-name)]
        (if (ext/is-extension (last re-res))
            (list file-name "_default")
            (rest re-res))
        (list file-name "_default")))

(defn extension-split [file-name]
    "Given a file name (assumes host specifier already split off)
    returns a list where first item is the base filename and the
    second is the extension to apply"
    (if-let [re-res (re-find dot-underscore-split file-name)]
        (if (ext/is-extension (last re-res))
            (rest re-res)
            (list file-name "put"))
        (list file-name "put")))

(def my-fdqn (.getCanonicalHostName (java.net.InetAddress/getLocalHost)))
(defn fdqn-matches [hostname]
    (= my-fdqn hostname))

(def my-hostname (.getHostName (java.net.InetAddress/getLocalHost)))
(defn hostname-matches [hostname]
    (= my-hostname hostname))

(defn index-of-specified-entry [host-seq]
    "Given a list of host specifiers, returns index of most specific one, or nil
    if none match"
    (util/thread-until host-seq
        (partial util/index-when fdqn-matches)
        (partial util/index-when hostname-matches)
        (partial util/index-when #(= "_default" %))))

(defmulti explode-file seq?)
(defmethod explode-file false [file-name]
    (let [spec-split-ret (host-specifier-split file-name)
          specifier      (last spec-split-ret)
          ext-split-ret  (extension-split (first spec-split-ret))
          extension      (last  ext-split-ret)
          base-name      (first ext-split-ret)]
    { :base-name base-name
      :specifier specifier
      :extension extension
      :full-name file-name
      :is-dir?   false    }))

(defmethod explode-file true [dir]
    (let [dir-name       (first dir)
          dir-ls         (rest  dir)
          spec-split-ret (host-specifier-split dir-name)
          specifier      (last spec-split-ret)
          ext-split-ret  (extension-split (first spec-split-ret))
          extension      (last  ext-split-ret)
          base-name      (first ext-split-ret)]
    { :base-name base-name
      :specifier specifier
      :extension extension
      :full-name dir-name
      :dir-ls    dir-ls 
      :is-dir?   true    }))

(defn add-to-basename-group 
    "Given a map and a file struct, creates a list with the file-struct
    as the only item for the index of the map corresponding to the file-
    struct's :base-name. If the list already exists in the given map,
    append to it instead"
    [file-map file-struct]
    (let [base-name (file-struct :base-name)
          base-name-seq (file-map base-name '())
          new-seq (cons file-struct base-name-seq)]
        (assoc file-map base-name new-seq)))

(defn narrow-group
    "Given a list of file-structs, returns the struct with the most specific
    specifier"
    [group-seq]
    (let [specifiers (map #(% :specifier) group-seq)]
        (when-let [i (index-of-specified-entry specifiers)]
            (nth group-seq i))))

(declare specify-files)
(defn directory-consolidate
    "If file-struct has :dir-ls then it is a directory. Call specify-files
    on the value at :dir-ls, then cons the file-struct (with :dir-ls dissociated)
    to the front."
    [file-struct]
    (if (contains? file-struct :dir-ls)
        (cons (dissoc file-struct :dir-ls) (specify-files (file-struct :dir-ls)))
        file-struct))

(defn specify-files 
    "Given a list of file names (presumably all in the same folder) goes and performs
    all the steps needed to narrow down which files we want to actually use for this
    particular node, and identifies which extension we want to process them with.
    Returns a list of file-structs"
    [file-list]
    (->> file-list
         (map explode-file)                 ; - Explode the files into their file-struct's
         (remove #(empty? (% :base-name)))  ; - Remove ones with empty base-names (shouldn't really happen)
         (reduce add-to-basename-group {})  ; - Reduce the structs by basename into a grouplist map
         (map #(narrow-group (val %)))      ; - Take all the vals in the grouplist map and narrow them down to a single file-struct
         (map directory-consolidate)))      ; - Call specify files on the file list of all directory structs

(defn specify-tree
    "Since specify-files expects simply a list of files, not a '(dir & files)
    structure like tree returns, we have to hack it a bit to make it work as
    expected"
    [dir]
    (->> (tree dir)
         (list)
         (specify-files)
         (first)))

(defn chroot-tree
    [root dir-tree]
    "Makes sure that the entire tree is chrooted to a directory. The given directory
    should not end in a /. If you want to chroot to root itself (/) pass in a blank
    string"
    (let [root-node  (first dir-tree)]
        (cons (assoc root-node :base-name root) (rest dir-tree))))

(defn append-slash [dir-name] (str dir-name "/"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; I couldn't explain again how these work, but they do. You give tree-map a
; function and a tree. The function should take three args, the first is a
; tree, the second the absolute prefix for this tree, and three the local
; duffel project prefix for this tree. The function should return the tree
; modified to your liking. When returned tree map will find all sub-trees
; (directories) in the tree you returned and run tree-map on them again, with
; aboslute and local prefixes updated accordingly. I'm leaving test-map around
; so you can see an example. It'll give each directory struct an :abs and a
; :local key so you can see what the list was being run with.
;
(declare _tree-map)
(defn tree-map
    [user-fn dir-tree]
    (let [ root-node  (first dir-tree)
           root-abs   (root-node :base-name)
           root-local "" ]
        (_tree-map user-fn (chroot-tree "" dir-tree) root-abs root-local)))

(defn _tree-map
    [user-fn dir-tree abs local]
    (let [ new-dir-tree      (user-fn dir-tree abs local)
           new-dir-tree-node (first new-dir-tree)
           new-dir-base-name (new-dir-tree-node :base-name)
           new-abs           (append-slash (str abs   (new-dir-tree-node :base-name)))
           new-local         (append-slash (str local (new-dir-tree-node :full-name))) ]
        (map #(if (seq? %)
                  (_tree-map user-fn % new-abs new-local)
                  %)
             new-dir-tree)))

(defn test-map [dir-tree abs-prefix local-prefix]
    (cons
        (assoc (first dir-tree) :abs   abs-prefix
                                :local local-prefix )
        (rest dir-tree)))
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn basename-is?
    [file-struct filename]
    (and (not (seq? file-struct)) (= (file-struct :base-name) filename)))

(defn merge-meta
    "Given a file-struct and some metadata merges the file-struct's meta field
    with the given metadata"
    [file-struct meta-struct]
    (assoc file-struct :meta (merge (file-struct :meta {}) meta-struct)))

(defn assoc-meta
    "Given a dir-tree looks through the dir-tree for a file with :base-name == filename
    and merges the given meta-struct with that file's :meta field. If the filename is .
    then apply the merge on the top level item (the directory struct). Does not go recursively
    down the tree."
    [dir-tree filename meta-struct]
    (if (= "." filename)
        (cons (merge-meta (first dir-tree) meta-struct) (rest dir-tree))
        (cons (first dir-tree) (map #(if (basename-is? % filename)
                                     (merge-meta % meta-struct)
                                     %) (rest dir-tree)))))

(defn pull-meta-file
    "Given a dir-tree and the local prefix for the dir-tree, tries to find a _meta.json in the tree.
    If found we remove the meta file-struct from the dir-tree and try to read the file from the disk,
    returning the filtered dir-tree and the contents of the _meta.json file, or the original dir-tree
    and '{}' if no _meta.json file was found"
    [dir-tree local-prefix]
    (if-let [meta-file (some #(when (basename-is? % "_meta.json") %) dir-tree)]
        [ (remove #(basename-is? % "_meta.json") dir-tree)
          (slurp (str local-prefix (meta-file :full-name))) ]
        [ dir-tree "{}" ]))

(def test-tree
    (->> (specify-tree "my-duffel")
         (chroot-tree "/tmp")))
