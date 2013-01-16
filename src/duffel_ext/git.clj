(ns duffel-ext.git
    (:use duffel.ext-protocol)
    (:require [duffel.fs-util :as dfs-util]
              [duffel.ext     :as dext]
              [duffel.backup  :as dbackup])
    (:require [duffel-ext.put :as dput]))

(defn git-exec-dir [git-user dir & args]
    (let [command-args (cons "git" args)]
        (cond
            (= git-user dput/current-username)
                (apply dfs-util/exec-in dir command-args)
            (= "root" dput/current-username)
                (apply dfs-util/exec-in dir "sudo" "-u" git-user command-args)
            :else
                (throw (Exception. (str "Could not switch user to " git-user))))))

(deftype git-ext [] duffel-extension

    (preprocess-file [x file-tree] file-tree)
    (preprocess-dir  [x dir-tree]
        (if-let [git-user (dfs-util/get-dir-meta dir-tree :git_user)]
            (let [dt-with-owner (dfs-util/merge-meta-dir-reverse dir-tree {:owner git-user})
                  owner         (dfs-util/get-dir-meta dt-with-owner :owner)]
                (dfs-util/merge-meta-dir-reverse dt-with-owner {:group owner}))
            dir-tree))

    (file-meta-tpl [x] {})
    (dir-meta-tpl  [x]
        (merge (dir-meta-tpl (dput/->put-ext))
            { :git_url    (list :string)
              :git_user   (list :string (list :optional dput/default-username))
              :git_branch (list :string (list :optional false)) }))

    (postprocess-file [x file-struct] file-struct)
    (postprocess-dir  [x dir-struct]  dir-struct)

    (process-file [x app meta-struct abs local]
        (throw (Exception. "git extension doesn't support handling files")))

    (process-dir [x app meta-struct abs local]
        (let [git-url    (meta-struct :git_url)
              git-user   (meta-struct :git_user)
              git-branch (meta-struct :git_branch)]
             
            (if-not (dfs-util/exists? abs)
                (do
                    (println "Cloning" git-url "into" abs) (flush)
                    (git-exec-dir git-user "." "clone" git-url abs))
                (println abs "already exists, not cloning into it"))
            
            (when git-branch
                (if (re-find #"working directory clean" (git-exec-dir git-user abs "status"))
                    (do
                        (println "Checking out" abs "to" git-branch "branch") (flush)
                        (git-exec-dir git-user abs "checkout" git-branch)
                        (git-exec-dir git-user abs "fetch")
                        (git-exec-dir git-user abs "reset" "--hard" (str "origin/" git-branch)))
                    (throw (Exception. 
                        (str "Git project at " abs " is not clean, can't checkout branch")))))

            (dput/try-ownership abs meta-struct))
    ) 
)

(dext/register-ext "git" (->git-ext))
