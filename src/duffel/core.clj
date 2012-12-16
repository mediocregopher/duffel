(ns duffel.core
    (:gen-class :main true)
    (:use clojure.contrib.command-line)
    (:require [duffel.fs      :as dfs]
              [duffel.util    :as dutil]
              [duffel.ext     :as dext]
              [duffel.fs-util :as dfs-util]
              duffel-ext.put))

(defn parse-chroot
    "Make sure that chroot is properly formed"
    [chroot]
    (case chroot
        ""  ""
        "/" ""
        (dutil/remove-trailing-slash chroot)))

(defn run-on-dir
    [dir chroot]
    (dutil/doall*
        (->> (dfs/specify-tree dir)
             (dfs/translate-top-level)
             (dfs-util/chroot-tree chroot)
             (dfs-util/tree-map dfs/distribute-meta)
             (dfs-util/tree-map dfs/filter-git)
             (dext/pre-process)
             (dext/process-templates)
             (dext/post-template-process)
             (dext/process)
             ))
    nil)


(defn -main [& args]
    (with-command-line args
        "A simple resource deployment tool. Check the docs at https://github.com/mediocregopher/duffel for more details\n\nUsage: duffel [OPTIONS] <duffel-directory>\n"
        [[ chroot "Directory to chroot to" "/" ]
         remaining]
         (if-let [dir (first remaining)]
            (run-on-dir (first remaining) (parse-chroot chroot))
            (binding [*out* *err*]
                (println "A duffel directory must be specified")
                (System/exit 1)))))
