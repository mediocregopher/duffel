(ns duffel.fs-util
    (:use [clojure.string :only [split trim]])
    (:require [duffel.util :as dutil])
    (:import java.io.File))

(defn chroot-tree
    [root dir-tree]
    "Makes sure that the entire tree is chrooted to a directory. The given directory
    should not end in a /. If you want to chroot to root itself (/) pass in a blank
    string"
    (let [root-node  (first dir-tree)]
        (cons (assoc root-node :base-name root :is-root? true) (rest dir-tree))))

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
        (chroot-tree root-abs
            (_tree-map user-fn (chroot-tree "" dir-tree) root-abs root-local))))

(defn _tree-map
    [user-fn dir-tree abs local]
    (let [ new-dir-tree      (user-fn dir-tree abs local)
           new-dir-tree-node (first new-dir-tree)
           new-dir-base-name (new-dir-tree-node :base-name)
           new-abs           (dutil/append-slash (str abs   (new-dir-tree-node :base-name)))
           new-local         (dutil/append-slash (str local (new-dir-tree-node :full-name))) ]
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

(defn get-dir-meta
    "Given a dir-tree and a key, gets that key from the meta for the root"
    [dir-tree meta-key]
    (((first dir-tree) :meta) meta-key))

(defn merge-meta
    "Given a file-struct and some metadata merges the file-struct's meta field
    with the given metadata"
    [file-struct meta-file-struct]
    (assoc file-struct :meta (merge (file-struct :meta {}) meta-file-struct)))

(defn merge-meta-reverse
    "Same as merge-meta, but whatever's already in the file-struct takes precedence"
    [file-struct meta-file-struct]
    (assoc file-struct :meta (merge meta-file-struct (file-struct :meta {}))))

(defn _merge-meta-dir
    "Given a dir-tree and metadata, merges the dir-tree's root element's metadata
    with the given metadata"
    [dir-tree meta-file-struct merge-fn]
    (cons (merge-fn (first dir-tree) meta-file-struct) (rest dir-tree)))

(defn merge-meta-dir         [d m] (_merge-meta-dir d m merge-meta))
(defn merge-meta-dir-reverse [d m] (_merge-meta-dir d m merge-meta-reverse))

(defn mkdir-p 
    "Calls mkdir -p on the given directory"
    [dir]
    (.mkdirs (java.io.File. dir)))

(defn- print-return-stream
    [stream]
    (let [stream-seq (->> stream
                          (java.io.InputStreamReader.)
                          (java.io.BufferedReader.)
                          line-seq)]
        (doall (reduce
            (fn [acc line]
                (println line)
                (if (empty? acc) line (str acc "\n" line)))
            ""
            stream-seq))))

(defn exec-stream
    "Executes a command in the given dir, streaming stdout and stderr to stdout,
    and once the exec is finished returns a vector of the return code, a string of
    all the stdout output, and a string of all the stderr output"
    [dir command & args]
    (let [runtime  (Runtime/getRuntime)
          proc     (.exec runtime (into-array (cons command args)) nil (File. dir))
          stdout   (.getInputStream proc)
          stderr   (.getErrorStream proc)
          outfut   (future (print-return-stream stdout))
          errfut   (future (print-return-stream stderr))
          proc-ret (.waitFor proc)]
        [proc-ret @outfut @errfut]
        ))

(defn exec-in
    "Executes a command in the given dir, throws an exception if the command doesn't return
    an exit code of 0"
    [dir command & args]
    (let [runtime  (Runtime/getRuntime)
          proc     (.exec runtime (into-array (cons command args)) nil (File. dir))
          proc-ret (.waitFor proc)]
        (if (= 0 proc-ret)
            (slurp (.getInputStream proc))
            (throw (Exception. (slurp (.getErrorStream proc)))))))

(defn exec
    "Executes a command in cwd"
    [command & args]
    (apply exec-in (concat ["." command] args)))

(defn chmod 
    "Calls chmod on a file/directory"
    [perms fsitem]
    (exec "chmod" perms fsitem))

(defn chown
    "Calls chown on a file/directory"
    [user group fsitem]
    (exec "chown" (str user ":" group) fsitem))

(defn cp
    "Calls cp <src> <dst>"
    [src dst]
    (exec "cp" src dst))

(defn ls
    "Returns a list of filenames in given directory"
    [dir]
    (map #(.getName %) (.listFiles (File. dir))))

(defn rm-rf
    "Deletes the given file or directory"
    [filedir]
    (exec "rm" "-rf" filedir))

(defn touch
    [file]
    (exec "touch" file))

(defn exists?
    "Returns true or false for whether or not the given file exists"
    [file]
    (.exists (File. file)))

(defn permissions
    "Returns vector of file permissions, [full-octal owner group], where
    full-octal is the four number octal sequenct of the permissions (0755, 1655, etc...)"
    [file]
    (let [perm-str (trim (exec "stat" "-c" "%a %U %G" file))
          [perm owner group] (split perm-str #" ")]
        [(dutil/full-octal perm) owner group]))

(defn exact?
    "Returns true or false for if the two given files have the exact same contents"
    [file-a file-b]
    (try (exec "cmp" file-a file-b) true
    (catch Exception e false)))
