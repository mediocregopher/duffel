(ns duffel.ext
    (:require [duffel.fs-util :as dfs-util]
              [duffel-ext.put :as dput]))

(def extensions [ "put" "tpl" ])

(defn is-extension [suffix]
    (not (nil? (some #{suffix} extensions))))

; Preprocessing - used basically just for apply recursively and stuff, before the templates
;                 are applied. Can also be used to forcefully exclude files/dirs from having
;                 an extension applied on them (throw an exception here)
;
; Template-processing - Used to apply the file and dir templates where necessary. Not implemented
;                       by extension itself, only used tpl structs given by extension
;
; Actual processessing - Where the magic happens

(defn preprocess
    [dir-tree]
    (dfs-util/tree-map (fn [d _ _] (dput/preprocess-dir d)) dir-tree))
