(ns duffel.script
    (:use     [clojure.string :only [trim]])
    (:require [duffel.fs-util :as dfs-util]))

(defn scripts-dir
    [root-dir]
    (str root-dir "/_SCRIPTS"))

(defn script
    [root-dir script-name]
    (str (scripts-dir root-dir) "/" script-name))

(def exists?  dfs-util/exists?)
(def exec    dfs-util/exec)

(defn is-in-group?
    [root-dir group-name]
    (= "1" (trim (exec (script root-dir "is-in-group") group-name))))
