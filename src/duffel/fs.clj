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

(defmulti explode-file list?)
(defmethod explode-file false [file-name]
    (let [spec-split-ret (host-specifier-split file-name)
          specifier      (last spec-split-ret)
          ext-split-ret  (extension-split (first spec-split-ret))
          extension      (last  ext-split-ret)
          base-name      (first ext-split-ret)]
    { :base-name base-name
      :specifier specifier
      :extension extension
      :file-name file-name
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
      :file-name dir-name
      :dir-ls    dir-ls 
      :is-dir?   true    }))

(defn explode-files-in-list 
    "Take a list of filenames and turn them into file structs containing info
    about their real name, extension, and host specifier"
    [file-list]
    (map explode-file file-list))

(defn remove-empty-file-structs
    "Looks through list of file structs and removes any with empty names"
    [file-structs]
    (remove #(empty? (% :base-name)) file-structs))

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

(defn group-by-basename
    "Goes through file structs and groups them into a map by basename
    { <base-name> <list of structs> }"
    [file-structs]
    (reduce add-to-basename-group {} file-structs))

(defn narrow-group
    "Given a list of file-structs, returns the struct with the most specific
    specifier"
    [group-seq]
    (let [specifiers (map #(% :specifier) group-seq)]
        (when-let [i (index-of-specified-entry specifiers)]
            (nth group-seq i))))

(defn narrow-groups
    "Given a map of basename groups, returns a list of all the most specific
    structs from each basename group"
    [file-map]
    (map #(narrow-group (val %)) file-map))

(defn directory-consolidate
    "If file-struct has :dir-ls then it is a directory. Call specify-files
    on the value at :dir-ls, then cons the file-struct (with :dir-ls dissociated)
    to the front."
    [file-struct]
    (if (contains? file-struct :dir-ls)
        (cons (dissoc file-struct :dir-ls) (specify-files (file-struct :dir-ls)))
        file-struct))

(defn directory-recursion
    [file-structs]
    (map directory-consolidate file-structs))

(defn specify-files 
    "Given a list of file names (presumably all in the same folder) goes and performs
    all the steps needed to narrow down which files we want to actually use for this
    particular node, and identifies which extension we want to process them with.
    Returns a list of file-structs"
    [file-list]
    (->> file-list
         (explode-files-in-list)
         (remove-empty-file-structs)
         (group-by-basename)
         (narrow-groups)
         (directory-recursion)))
