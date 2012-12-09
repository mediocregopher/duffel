(ns duffel.fs
    (:require [duffel.ext     :as dext]
              [duffel.meta    :as dmeta]
              [duffel.util    :as dutil]
              [duffel.fs-util :as dfs-util])
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
        (if (dext/is-extension (last re-res))
            (list file-name "_default")
            (rest re-res))
        (list file-name "_default")))

(defn extension-split [file-name]
    "Given a file name (assumes host specifier already split off)
    returns a list where first item is the base filename and the
    second is the extension to apply"
    (if-let [re-res (re-find dot-underscore-split file-name)]
        (if (dext/is-extension (last re-res))
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
    (dutil/thread-until host-seq
        (partial dutil/index-when fdqn-matches)
        (partial dutil/index-when hostname-matches)
        (partial dutil/index-when #(= "_default" %))))

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

(defn basename-is?
    [file-struct filename]
    (and (not (seq? file-struct)) (= (file-struct :base-name) filename)))

(defn _assoc-meta
    "Given a dir-tree looks through the dir-tree for a file with :base-name == filename
    and merges the given meta-struct with that file's :meta field. If the filename is .
    then apply the merge on the top level item (the directory struct). Does not go recursively
    down the tree."
    [dir-tree filename meta-file-struct merge-fn merge-dir-fn]
    (if (= "." filename)
        (merge-dir-fn dir-tree meta-file-struct)
        (cons (first dir-tree)
            (map #(if (basename-is? % filename) (merge-fn % meta-file-struct)
                  (if (and (seq? %) 
                           (basename-is? (first %) filename)) (merge-dir-fn % meta-file-struct)
                   %))(rest dir-tree)))))

(defn assoc-meta         [d f m] (_assoc-meta d f m dfs-util/merge-meta 
                                                    dfs-util/merge-meta-dir))
(defn assoc-meta-reverse [d f m] (_assoc-meta d f m dfs-util/merge-meta-reverse 
                                                    dfs-util/merge-meta-dir-reverse))

(defn pull-meta-file
    "Given a dir-tree and the local prefix for the dir-tree, tries to find a _meta.json in the tree.
    If found we remove the meta file-struct from the dir-tree and try to read the file from the disk,
    returning the filtered dir-tree and the contents of the _meta.json file, or the original dir-tree
    and '{}' if no _meta.json file was found"
    [dir-tree local-prefix]
    (if-let [meta-file (some #(when (basename-is? % "_meta.json") %) dir-tree)]
        [ (remove #(basename-is? % "_meta.json") dir-tree)
          (slurp (str local-prefix (dfs-util/append-slash ((first dir-tree) :full-name)) (meta-file :full-name))) ]
        [ dir-tree "{}" ]))

(defn distribute-meta
    "A function to be passed into tree map which will find (and remove) all _meta.json files from the
    map, read them in, and apply the meta object inside of them to the appropriate file-structs"
    [dir-tree _ local-prefix]
    (let [ pull-meta-ret (pull-meta-file dir-tree local-prefix)
           new-dir-tree  (first  pull-meta-ret)
           meta-string   (second pull-meta-ret) ]
        (if-let [ meta-struct (dmeta/parse-meta-string meta-string) ]
            (reduce #(assoc-meta %1 (key %2) (val %2)) new-dir-tree meta-struct)
            (throw (Exception. (str "Could not parse _meta.json file in " 
                                    local-prefix ((first dir-tree) :full-name)
                                    ", it might not be valid json"))))))

(defn test-tree []
    (->> (specify-tree "my-duffel")
         (dfs-util/chroot-tree "/tmp")
         (dfs-util/tree-map distribute-meta)
         (dext/preprocess)
         ))
