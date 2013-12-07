(ns duffel.script
    (:use     [clojure.string :only [trim]])
    (:require [duffel.fs :refer [exists? exec-in]]))

(defn exec-script
  "Runs root-dir/_SCRIPTS/script-name with args. Returns nil if script doesn't
  actually exist, otherwise returns output from the execution"
  [app script-name & args]
  (let [script-rel-path (str (app :proj-root) "scripts/" script-name)]
    (when (exists? script-rel-path)
      (apply exec-in (app :proj-root) script-rel-path args))))

(defn is-in-group?
  [app group-name]
  "Returns whether or not this node is in the given group"
  (if-let [ret (exec-script app "is-in-group" group-name)]
    (= "1" (trim ret))
    false))
