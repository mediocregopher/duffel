(ns duffel.fs
    (:use [clojure.string :only [split trim]])
    (:require [duffel.util :as dutil])
    (:import java.io.File))

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
    and once the exec is finished returns a vector of the return code, a string
    of all the stdout output, and a string of all the stderr output"
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
    "Executes a command in the given dir, throws an exception if the command
    doesn't return an exit code of 0"
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
    (apply exec-in "." command args))

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

(defn lns
    "Calls ln -s <src> <dst>"
    [src dst]
    (exec "ln" "-s" src dst))

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

(defn darwin?
  "Know if we are on a Mac, since certain unix tools are slightly different"
  []
  (= (trim (exec "uname")) "Darwin"))

(defn find-default-group
  "Chooses the default group as the group of the user's home,
  or if all else fails uses the username"
  [username]
  (try
    (trim
     (if (darwin?)
       (exec "stat" "-f" "%Sg" (System/getenv "HOME"))
       (exec "stat" "-c" "%G" (System/getenv "HOME"))))
    (catch Exception e
      ;; Fuck it, let's just return the username and pretend this never happened...
      username)))

(defn permissions
    "Returns vector of file permissions, [full-octal owner group], where
    full-octal is the four number octal sequenct of the permissions (0755, 1655,
    etc...)"
    [file]
    (let [perm-str (trim
                     (if (darwin?)
                       (exec "stat" "-f" "%OLp %Su %Sg" file)
                       (exec "stat" "-c" "%a %U %G" file)))
          [perm owner group] (split perm-str #" ")]
        [(dutil/full-octal perm) owner group]))

(defn get-full-path [path]
  (.getAbsolutePath  (java.io.File. path)))

(defn exact?
    "Returns true or false for if the two given files have the exact same
    contents"
    [file-a file-b]
    (try (exec "cmp" file-a file-b) true
    (catch Exception e false)))
