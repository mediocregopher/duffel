(ns duffel.fs
    (:require [duffel.ext     :as dext]
              [duffel.meta    :as dmeta]
              [duffel.util    :as dutil]
              [duffel.fs-util :as dfs-util]
              [duffel.translation :as dtran]
              [duffel.script  :as dscript]
              [clojure.string :as s]
              [clojure.walk :refer [walk]])
    (:import java.io.File))

(def my-fdqn (.getCanonicalHostName (java.net.InetAddress/getLocalHost)))
(defn fdqn-matches [hostname]
    (= my-fdqn hostname))

(def my-hostname (.getHostName (java.net.InetAddress/getLocalHost)))
(defn hostname-matches [hostname]
    (= my-hostname hostname))

(defn groupname-matches [proj-root groupname]
    (dscript/is-in-group? proj-root groupname))

(defn index-of-specified-entry [proj-root host-seq]
    "Given a list of host specifiers, returns index of most specific one, or nil
    if none match"
    (dutil/thread-until host-seq
        (partial dutil/index-when fdqn-matches)
        (partial dutil/index-when hostname-matches)
        (partial dutil/index-when #(groupname-matches proj-root %))
        (partial dutil/index-when #(= "_default" %))))

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
    "Given a list of file-structs, returns the struct with the most specific
    specifier"
    [proj-root group-seq]
    (let [specifiers (map #(% :specifier) group-seq)]
        (when-let [i (index-of-specified-entry proj-root specifiers)]
            (nth group-seq i))))

(declare specify-files)
(defn directory-consolidate
    "If file-struct has :dir-ls then it is a directory. Call specify-files on
    the value at :dir-ls, then cons the file-struct (with :dir-ls dissociated)
    to the front."
    [proj-root file-struct]
    (if (contains? file-struct :dir-ls)
        (cons (dissoc file-struct :dir-ls) (specify-files proj-root (file-struct :dir-ls)))
        file-struct))

(defn specify-files
    "Given a list of file names (presumably all in the same folder) goes and
    performs all the steps needed to narrow down which files we want to actually
    use for this particular node, and identifies which extension we want to
    process them with.  Returns a list of file-structs"
    [proj-root file-list]
    (->> file-list
         ; - Explode the files into their file-struct's
         (map explode-file)
         ; - Remove ones with empty base-names (shouldn't really happen)
         (remove #(empty? (% :base-name)))
         ; - Reduce the structs by basename into a grouplist map
         (reduce add-to-basename-group {})
         ; - Take all the vals in the grouplist map and narrow them down to a
         ;   single file-struct
         (map #(narrow-group proj-root (val %)))
         ; - narrow-group returns nil if group can't be narrowed, remove those
         ;   nils
         (remove nil?)
         ; - Call specify files on the file list of all directory structs
         (map (partial directory-consolidate proj-root))))

(defn specify-tree
    "Since specify-files expects simply a list of files, not a '(dir & files)
    structure like tree returns, we have to hack it a bit to make it work as
    expected"
    [dir]
    (->> (tree dir)
         (list)
         (specify-files dir)
         (first)))

(defn basename-matches?
    [file-struct fileglob]
    (let [regex-str (s/replace fileglob "*" ".*")
          regex (re-pattern regex-str)]
        (and (not (seq? file-struct))
             (not (nil? (re-find regex (file-struct :base-name)))))
    ))

(defn _assoc-meta
    "Given a dir-tree looks through the dir-tree for files where :base-name
    matches fileglob and merges the given meta-struct with that file's :meta
    field. If the fileglob is .  then apply the merge on the top level item (the
    directory struct). Does not go recursively down the tree."
    [dir-tree fileglob meta-file-struct merge-fn merge-dir-fn]
    (if (= "." fileglob) (merge-dir-fn dir-tree meta-file-struct)
        (cons (first dir-tree)
            (map #(if (and (seq? %) (basename-matches? (first %) fileglob)) (merge-dir-fn % meta-file-struct)
                  (if (basename-matches? % fileglob) (merge-fn % meta-file-struct)
                   %)) (rest dir-tree)))))

(defn assoc-meta         [d f m] (_assoc-meta d f m
                                    dfs-util/merge-meta
                                    dfs-util/merge-meta-dir))
(defn assoc-meta-reverse [d f m] (_assoc-meta d f m
                                    dfs-util/merge-meta-reverse
                                    dfs-util/merge-meta-dir-reverse))

(defn pull-meta-file
    "Given a dir-tree and the local prefix for the dir-tree, tries to find a
    _meta.json in the tree.  If found we remove the meta file-struct from the
    dir-tree and try to read the file from the disk, returning the filtered
    dir-tree and the contents of the _meta.json file, or the original dir-tree
    and '{}' if no _meta.json file was found"
    [dir-tree local-prefix]
    (if-let [meta-file (some #(when (basename-matches? % "_meta.json") %) dir-tree)]
        [ (remove #(basename-matches? % "_meta.json") dir-tree)
          (slurp (str
            local-prefix
            (dutil/append-slash ((first dir-tree) :full-name))
            (meta-file :full-name))) ]
        [ dir-tree "{}" ]))

(defn distribute-meta
    "A function to be passed into tree map which will find (and remove) all
    _meta.json files from the map, read them in, and apply the meta object
    inside of them to the appropriate file-structs"
    [dir-tree _ local-prefix]
    (let [ [new-dir-tree meta-string] (pull-meta-file dir-tree local-prefix) ]
        (if-let [ meta-struct (dmeta/parse-meta-string meta-string) ]
            (reduce #(assoc-meta %1 (key %2) (val %2)) new-dir-tree meta-struct)
            (throw (Exception. (str "Could not parse _meta.json file in "
                                    local-prefix ((first dir-tree) :full-name)
                                    ", it might not be valid json"))))))

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
