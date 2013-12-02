(ns duffel.backup
    (:require [duffel.fs-util :as dfs-util]
              [duffel.util    :as dutil]))

(defn setup-backup-dir
    "Sets up (creates) backup directory"
    [backup]
    (dfs-util/mkdir-p backup))

(defn prepend-backup
    [abs backup]
    (str (dutil/remove-trailing-slash backup) abs))

(defn like-filename
    [filename pot-filename]
    (when (re-find (re-pattern (str filename "\\.[0-9]+$")) pot-filename) true))

(defn append-timestamp
    [filename]
    (str filename "." (int (/ (System/currentTimeMillis) 1000))))

(defn ls-backups
    [full-backup-dir filename]
    (->> full-backup-dir
         (dfs-util/ls)
         (filter (partial like-filename filename))))

(defn prepare-backup
    "Given an absolute directory path to a file (assumes ends in slash), a
    filename, the path to the backup root, and the number of files allowed to be
    backed up, creates the backup directory for the given file and makes sure
    there's no more then backup-count-1 versions of the file already backed up"
    [abs-dir filename backup backup-count]
    (let [ full-backup-dir  (prepend-backup abs-dir backup)
           backup-filenames (ls-backups full-backup-dir filename) ]
        (when (> backup-count 0) (dfs-util/mkdir-p full-backup-dir))
        (->> backup-filenames
             (sort #(compare %2 %1))
             (drop (dec backup-count))
             (map #(str full-backup-dir %))
             (map dfs-util/rm-rf))))

(defn backup-file
    "Given an absolute directory path to a file (assumes ends in slash), a
    filename, the path to the backup root, and the number of files allowed to be
    backed up, creates a backup file while at the same time rotating out old
    files"
    [abs-dir filename backup-dir backup-count]
    (setup-backup-dir backup-dir)
    (doall (prepare-backup abs-dir filename backup-dir backup-count))
    (when (and (> backup-count 0) (dfs-util/exists? (str abs-dir filename)))
        (dfs-util/cp (str abs-dir filename)
                     (str (prepend-backup abs-dir backup-dir)
                          (append-timestamp filename)))))
