(ns duffel.ext-util
    (:require [duffel.fs-util :as dfs-util]
              [duffel.backup  :as dbackup]
              [duffel.util    :as dutil]))

(def current-username (java.lang.System/getProperty "user.name"))
(def default-username current-username)
(def default-group    (dfs-util/find-default-group default-username)) ;Assume primary group = user's group, cause fuck it

(def file-ownership-tpl 
    { :chmod (list :string (list :optional "0644") '(:regex #"^[0-7]{3,4}$"))
      :owner (list :string (list :optional default-username)                )
      :group (list :string (list :optional default-group)                   ) })

(def dir-ownership-tpl
    (merge file-ownership-tpl
        { :chmod (list :string (list :optional "0755") '(:regex #"^[0-7]{3,4}$")) }))
        
(defn force-ownership
    [abs meta-struct]
    (dfs-util/chown (meta-struct :owner) (meta-struct :group) abs)
    (dfs-util/chmod (meta-struct :chmod) abs))

(defn try-ownership
    [abs meta-struct]
    ;We try to do the ownership stuff, but if we can't and force_ownership isn't on
    ;we don't worry about it
    (try (force-ownership abs meta-struct)
    (catch Exception e
        (when (meta-struct :force_ownership) (throw e)))))

(defn try-backup
    [app abs]
    (when-not (app :no-backup) 
        (let [ [abs-dir filename] (dutil/path-split abs) ]
            (dbackup/backup-file abs-dir filename (app :backup-dir) (app :backup-count)))))

(defn perm-same?
    "Looks at a pre-existing file/dir and the meta-struct that's going to be applied to it, and
    returns true/false if the existing permissions are different"
    [abs meta-struct]
    (let [local-perms [ (dutil/full-octal (meta-struct :chmod))
                        (meta-struct :owner)
                        (meta-struct :group) ]]
        (= local-perms (dfs-util/permissions abs))))

(defn dirs-no-difference?
    [local abs meta-struct]
    (and (dfs-util/exists? abs)
         (perm-same? abs meta-struct)))

(defn files-no-difference?
    [local abs meta-struct]
    (and (dfs-util/exists? abs)
         (perm-same? abs meta-struct)
         (dfs-util/exact? local abs)))

(defn print-fs-action [action local abs meta-struct]
    (println action local "->" abs "::" 
        (meta-struct :chmod) (str (meta-struct :owner) ":" (meta-struct :group))))

