(ns duffel.core
  (:gen-class :main true)
  (:use clojure.contrib.command-line)
  (:require [duffel.ext.core     :refer [get-impl process-dir process-file]]
            duffel.ext.load
            [duffel.tree.core    :refer [dir->tree tree-map tree-get]]
            [duffel.tree.paths   :refer [fill-paths]]
            [duffel.tree.specify :refer [specify-tree]]
            [duffel.tree.meta    :refer [collapse-meta cascade-meta]]))

(defn doall*
    "Same as doall, but recursive"
    [s] (dorun (tree-seq seq? seq s)) s)

(defn str->int
    "Given a string, parses the first int out of it possible, or nil if none
    found"
    [s]
    (try (Integer/valueOf s) (catch Exception e nil)))

(defn process-el
  "Given the app and either a duffel tree or a file-map, runs that items chosen
  extension on it, returning the result"
  [app el]
  (if-let [ext-impl (get-impl (tree-get el :extension))]
    (if (sequential? el)
      (process-dir ext-impl app el)
      (process-file ext-impl app el))
    el))

(defn process-exts
  "Processes all extensions on a duffel tree"
  [app dtree]
  (tree-map
    #(cons
      (process-el app (first dtree))
      (map (partial process-el app) (rest dtree)))
    dtree))

(defn run
  "Runs the app itself, taking in the app map. It will create the duffel tree
  for this project, process it, and handle all extensions"
  [app]
  (try
    (println "starting duffel run")
    (doall*
      (->> (dir->tree (str (app :proj-root) "/root/"))
           (fill-paths app)
           (specify-tree app)
           (collapse-meta)
           (cascade-meta)
           (process-exts app)))
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
        (run { :proj-root (if-not (= (last dir) \/) (str dir "/") dir)
               :chroot chroot
               :no-backup no-backup?
               :backup-dir backup-directory
               :backup-count (str->int backup-count) }
        (System/exit 0)))
      (binding [*out* *err*]
          (println "A duffel directory must be specified")
          (System/exit 1)))))
