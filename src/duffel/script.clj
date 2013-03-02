(ns duffel.script
    (:use     [clojure.string :only [trim]])
    (:require [duffel.fs-util :as dfs-util]))

(defn exec-script
    "Runs root-dir/_SCRIPTS/script-name with args. Returns nil if script doesn't
    actually exist, otherwise returns output from the execution"
    [root-dir script-name & args]
    (let [script-rel-path (str "_SCRIPTS/" script-name)]
        (when (dfs-util/exists? (str root-dir "/" script-rel-path))
            (apply dfs-util/exec-in root-dir script-rel-path args))))

(defn is-in-group?
    [root-dir group-name]
    (if-let [ret (exec-script root-dir "is-in-group" group-name)]
        (= "1" (trim ret))
        false))
