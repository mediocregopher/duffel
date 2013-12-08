(ns duffel.ext.util
    (:require [duffel.fs      :as dfs]
              [duffel.tree.core :refer [tree-get-in]]))

(def current-username (java.lang.System/getProperty "user.name"))
(def default-username current-username)
;Assume primary group = user's group, cause fuck it
(def default-group    (dfs/find-default-group default-username))

(defn meta-get-in
  "Same as tree-get-in, but automatically descends down into the meta key"
  [el ks & default]
  (apply tree-get-in el (cons :meta ks) default))

(defn meta-get
  "Same as tree-get, but automatically descends down into the meta key"
  [el k & default]
  (apply meta-get-in el [k] default))

(defn file-filled-perms
  "Given a file-map, returns the owner, group, and chmod specified by its
  metadata, or the defaults there-of if they're not given"
  [file-map]
  [ (tree-get-in file-map [:meta "owner"] default-username)
    (tree-get-in file-map [:meta "group"] default-group)
    (tree-get-in file-map [:meta "chmod"] "0644") ])

(defn dir-filled-perms
  "Given a dtree, returns the owner, group, and chmod specified by its metadata,
  or the defaults there-of if they're not given"
  [dtree]
  [ (tree-get-in dtree [:meta "owner"] default-username)
    (tree-get-in dtree [:meta "group"] default-group)
    (tree-get-in dtree [:meta "chmod"] "0755") ])

(defn force-perms
  "Forces the item at the given absolute path to have the given permissions"
  [abs owner group chmod]
  (dfs/chown owner group abs)
  (dfs/chmod chmod abs))

(defn perm-same?
  "Looks at a pre-existing file/dir and its perms that are going to be applied
  to it, and returns true/false if the existing permissions are different"
  [abs owner group chmod]
  (and (dfs/exists? abs)
       (= [owner group (dfs/full-octal chmod)] (dfs/permissions abs))))

(defn files-same?
  "Looks at a local file and its potential permissions and one on the
  filesystem, and returns whether or not they are exactly the same already"
  [local abs owner group chmod]
  (and (perm-same? abs owner group chmod)
       (dfs/exact? local abs)))

(defn print-fs-action
  "Used to print out a line describing what's happening to a local file"
  [action local abs owner group chmod]
  (println action local "->" abs "::" chmod (str owner ":" group)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Backup stuff
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- setup-backup-dir
    "Sets up (creates) backup directory"
    [backup]
    (dfs/mkdir-p backup))

(defn- remove-trailing-slash
  "Given a string, if it ends in a slash, removes it"
  [dir-name]
  (if (= \/ (last dir-name))
      (apply str (butlast dir-name))
      dir-name))

(defn- prepend-backup
    [abs backup]
    (str (remove-trailing-slash backup) abs))

(defn- like-filename
    [filename pot-filename]
    (when (re-find (re-pattern (str filename "\\.[0-9]+$")) pot-filename) true))

(defn- append-timestamp
    [filename]
    (str filename "." (int (/ (System/currentTimeMillis) 1000))))

(defn- ls-backups
    [full-backup-dir filename]
    (->> full-backup-dir
         (dfs/ls)
         (filter (partial like-filename filename))))

(defn- prepare-backup
    "Given an absolute directory path to a file (assumes ends in slash), a
    filename, the path to the backup root, and the number of files allowed to be
    backed up, creates the backup directory for the given file and makes sure
    there's no more then backup-count-1 versions of the file already backed up"
    [abs-dir filename backup backup-count]
    (let [ full-backup-dir  (prepend-backup abs-dir backup)
           backup-filenames (ls-backups full-backup-dir filename) ]
        (when (> backup-count 0) (dfs/mkdir-p full-backup-dir))
        (->> backup-filenames
             (sort #(compare %2 %1))
             (drop (dec backup-count))
             (map #(str full-backup-dir %))
             (map dfs/rm-rf))))

(defn- backup-file
    "Given an absolute directory path to a file (assumes ends in slash), a
    filename, the path to the backup root, and the number of files allowed to be
    backed up, creates a backup file while at the same time rotating out old
    files"
    [abs-dir filename backup-dir backup-count]
    (setup-backup-dir backup-dir)
    (doall (prepare-backup abs-dir filename backup-dir backup-count))
    (when (and (> backup-count 0) (dfs/exists? (str abs-dir filename)))
        (dfs/cp (str abs-dir filename)
                     (str (prepend-backup abs-dir backup-dir)
                          (append-timestamp filename)))))

(defn try-backup
  "Attempts to backup the file at the given absolute path, using the app's
  options as a guide for whether or not to actually do it, and where to put the
  backup if it should"
  [app abs]
  (when-not (app :no-backup)
    (let [ [abs-dir filename] (rest (re-find #"(.+?)([^\/]*)$" abs)) ]
      (backup-file
        abs-dir
        filename
        (app :backup-dir)
        (app :backup-count)))))

