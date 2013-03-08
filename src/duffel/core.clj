(ns duffel.core
    (:gen-class :main true)
    (:use clojure.contrib.command-line)
    (:require [duffel.fs      :as dfs]
              [duffel.util    :as dutil]
              [duffel.ext     :as dext]
              [duffel.fs-util :as dfs-util]
              duffel-ext.put
              duffel-ext.git
              duffel-ext.ignore
              duffel-ext.touch
              duffel-ext.clerb))

(defn parse-chroot
    "Make sure that chroot is properly formed"
    [chroot]
    (case chroot
        ""  ""
        "/" ""
        (dutil/remove-trailing-slash chroot)))

(defn run-on-dir
    [dir app]
    (dutil/doall*
        (->> (dfs/specify-tree dir)
             (dfs/translate-top-level)
             (dfs/remove-special-dirs)
             (dfs-util/chroot-tree (app :chroot))
             (dfs-util/tree-map dfs/distribute-meta)
             (dfs-util/tree-map dfs/filter-git)
             (dext/pre-process)
             (dext/process-templates)
             (dext/post-template-process)
             (dext/process app)
             ))
    nil)


(defn -main [& args]
    (with-command-line args
        "A simple resource deployment tool. Check the docs at https://github.com/mediocregopher/duffel for more details\n\nUsage: duffel [OPTIONS] <duffel-directory>\n"
        [[ chroot "Directory to chroot to" "/" ]
         [ no-backup? n? "Set if you don't want to do any backing up of files" ]
         [ backup-directory b "Directory to backup files to" "/tmp/duffel_bak" ]
         [ backup-count "Number of backup files to keep" "3" ]
         remaining]
         (if-let [dir (first remaining)]
            (run-on-dir dir { :chroot (parse-chroot chroot)
                              :no-backup no-backup?
                              :backup-dir backup-directory
                              :backup-count (dutil/str->int backup-count) })
            (binding [*out* *err*]
                (println "A duffel directory must be specified")
                (System/exit 1)))))
