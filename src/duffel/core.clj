(ns duffel.core
    (:gen-class :main true)
    (:use clojure.contrib.command-line)
    (:require [duffel.fs      :as dfs]
              [duffel.ext     :as dext]
              [duffel.fs-util :as dfs-util]
              duffel-ext.put
              duffel-ext.git
              duffel-ext.ignore
              duffel-ext.touch
              duffel-ext.dlns
              duffel-ext.clerb
              duffel-ext.pacman))

(defn doall*
    "Same as doall, but recursive"
    [s] (dorun (tree-seq seq? seq s)) s)

(defn str->int
    "Given a string, parses the first int out of it possible, or nil if none
    found"
    [s]
    (try (Integer/valueOf s) (catch Exception e nil)))

(defn parse-chroot
    "Make sure that chroot is properly formed"
    [chroot]
    (case chroot
        ""  ""
        "/" ""
        (dutil/remove-trailing-slash chroot)))

(defn run-on-dir
    [dir app]
    (try
      (println "starting duffel run")
      (doall*
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
      (println "duffel run complete!")
      nil
    (catch Exception e (.printStackTrace e) (System/exit 1))))

(def cli-help "A simple resource deployment tool. Check the docs at
https://github.com/mediocregopher/duffel for more details\n\nUsage: duffel
[OPTIONS] <duffel-directory>\n" )


(defn -main [& args]
    (with-command-line args
        cli-help
        [[ chroot "Directory to chroot to" "/" ]
         [ no-backup? n? "Set if you don't want to do any backing up of files" ]
         [ backup-directory b "Directory to backup files to" "/tmp/duffel_bak" ]
         [ backup-count "Number of backup files to keep" "3" ]
         remaining]
         (if-let [dir (first remaining)]
            (do
              (run-on-dir dir { :chroot (parse-chroot chroot)
                                :no-backup no-backup?
                                :backup-dir backup-directory
                                :backup-count (dutil/str->int backup-count) })
              (System/exit 0))
            (binding [*out* *err*]
                (println "A duffel directory must be specified")
                (System/exit 1)))))
