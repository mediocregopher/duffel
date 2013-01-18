(ns duffel.ext-util
    (:require [duffel.fs-util :as dfs-util]))

(def current-username (java.lang.System/getProperty "user.name"))
(def default-username current-username)
(def default-group    default-username) ;Assume primary group = username, cause fuck it

(def file-ownership-tpl 
    { :chmod (list :string (list :optional "0644") '(:regex #"^[0-7]{3,4}$"))
      :owner (list :string (list :optional default-username)                )
      :group (list :string (list :optional default-group)                   ) })

(def dir-ownership-tpl
    (merge file-ownership-tpl
        { :chmod (list :string (list :optional "0755") '(:regex #"^[0-7]{3,4}$")) }))
        

(defn try-ownership
    [abs meta-struct]
    ;We try to do the ownership stuff, but if we can't and force_ownership isn't on
    ;we don't worry about it
    (try
        (dfs-util/chown (meta-struct :owner) (meta-struct :group) abs)
        (dfs-util/chmod (meta-struct :chmod) abs)
    (catch Exception e
        (when (meta-struct :force_ownership) (throw e)))))

(defn print-fs-action [action local abs meta-struct]
    (println action local "->" abs "::" 
        (meta-struct :chmod) (str (meta-struct :owner) ":" (meta-struct :group))))

