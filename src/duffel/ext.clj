(ns duffel.ext
    (:use massage.json)
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
; Postprocessessing - Same thing as preprocessing. I'm not actually using it, but someone else
;                     might
;
; Actual processessing - Where the magic happens

(defn pre-process
    "Goes through the dir-tree and, using tree-map, calls preprocess-dir on all directories
    and preprocess-file on all files"
    [dir-tree]
    (dfs-util/tree-map (fn [d _ _] 
        (let [processed-d (dput/preprocess-dir d)
              processed-root (first processed-d)]
            (cons processed-root
                (map #(if (seq? %) % (dput/preprocess-file %)) (rest processed-d))))
    ) dir-tree))

;I'll consolidate all these process functions once I figure out how I wanna do the detecting extension
;and deciding on the function call based on that
(defn post-template-process
    "Goes through the dir-tree and, using tree-map, calls postprocess-dir on all directories
    and preprocess-file on all files"
    [dir-tree]
    (dfs-util/tree-map (fn [d _ _] 
        (let [processed-d (dput/postprocess-dir d)
              processed-root (first processed-d)]
            (cons processed-root
                (map #(if (seq? %) % (dput/postprocess-file %)) (rest processed-d))))
    ) dir-tree))

(defn process
    [dir-tree]
    (dfs-util/tree-map 
        (fn [d abs-prefix local-prefix] 
            (let [ d-root         (first d)
                   d-abs          (str abs-prefix   (d-root :base-name))
                   d-local        (str local-prefix (d-root :full-name))
                   d-abs-prefix   (dfs-util/append-slash d-abs)
                   d-local-prefix (dfs-util/append-slash d-local) ]
                (dput/process-dir (d-root :meta) d-abs d-local)
                (doseq [f (rest d)] (when-not (seq? f) 
                    (dput/process-file (f :meta) (str d-abs-prefix   (f :base-name)) 
                                                 (str d-local-prefix (f :full-name))))))
        d) 
        dir-tree))

(defn _try-apply-template 
    "Given a file-struct and a template, tries to run the metadata of the file-struct
    through massage to populate all defaults and make sure everything is ok. If there's
    any errors it throws an exception, which is what the local-prefix is needed for.

    There's some defmulti stuff below to make it easy to pick a template depending on
    whether the file-struct is for a directory or not"
    [file-struct local-prefix tpl]
    (let [processed-meta (parse-json (file-struct :meta {}) tpl)]
        (if (processed-meta :error)
            (throw (Exception. (str "Invalid metadata on: " 
                                    local-prefix
                                    (file-struct :full-name)
                                    " -> " processed-meta)))
            (assoc file-struct :meta processed-meta))))

(defmulti try-apply-template (fn [file-struct _] (file-struct :is-dir?)))
(defmethod try-apply-template false [file-struct local-prefix]
    (_try-apply-template file-struct local-prefix (dput/file-meta-tpl)))
(defmethod try-apply-template true [file-struct local-prefix]
    (_try-apply-template file-struct local-prefix (dput/dir-meta-tpl)))
    

(defn process-templates
    "Goes through the dir-tree and, using tree-map, sends all file and directory meta objects
    through their massage template to make sure they're good to go and populate defaults"
    [dir-tree]
    (dfs-util/tree-map (fn [d _ local-prefix]
        (let [ dir-root (first d)
               dir-rest (rest d)
               full-local-prefix (str local-prefix 
                                      (dfs-util/append-slash (dir-root :full-name))) ]
            (cons (try-apply-template dir-root full-local-prefix)
                  (map #(if (seq? %) % 
                            (try-apply-template % full-local-prefix)) dir-rest)))
    ) dir-tree))
