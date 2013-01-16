(ns duffel.script
    (:use     [clojure.string :only [trim]])
    (:require [duffel.fs-util :as dfs-util]))

(defn exec-script
    [root-dir script-name & args]
    (apply dfs-util/exec-in root-dir (str "_SCRIPTS/" script-name) args))

(defn is-in-group?
    [root-dir group-name]
    (= "1" (trim
        (exec-script root-dir "is-in-group" group-name))))
