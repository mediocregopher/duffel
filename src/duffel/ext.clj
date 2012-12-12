(ns duffel.ext
    (:use massage.json)
    (:use duffel.ext-protocol)
    (:require [duffel.fs-util :as dfs-util]
              [duffel-ext.put :as dput]))

(def extensions { "put" (dput/->put-ext) })

(defn is-extension [suffix]
    (contains? extensions suffix))

(defn ext-instance [file-struct]
    (extensions (file-struct :extension)))

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

(defn gen-process-structs
    "Goes through the dir-tree and, using tree-map, calls dir-fn on all directories
    and file-fn on all files"
    [dir-tree dir-fn file-fn]
    [dir-tree]
    (dfs-util/tree-map (fn [d _ _] 
        (let [ext (ext-instance (first d))
              processed-d (dir-fn ext d)
              processed-root (first processed-d)]
            (cons processed-root
                (map #(if (seq? %) % (file-fn (ext-instance %) %)) (rest processed-d))))
    ) dir-tree))

(defn pre-process
    "Goes through the dir-tree and, using tree-map, calls preprocess-dir on all directories
    and preprocess-file on all files"
    [dir-tree]
    (gen-process-structs dir-tree preprocess-dir preprocess-file))

(defn post-template-process
    "Goes through the dir-tree and, using tree-map, calls postprocess-dir on all directories
    and preprocess-file on all files"
    [dir-tree]
    (gen-process-structs dir-tree postprocess-dir postprocess-file))

(defn process
    [dir-tree]
    (dfs-util/tree-map 
        (fn [d abs-prefix local-prefix] 
            (let [ d-root         (first d)
                   d-root-ext     (ext-instance d-root)
                   d-abs          (str abs-prefix   (d-root :base-name))
                   d-local        (str local-prefix (d-root :full-name))
                   d-abs-prefix   (dfs-util/append-slash d-abs)
                   d-local-prefix (dfs-util/append-slash d-local) ]
                (when-not (d-root :is-root?) (process-dir d-root-ext (d-root :meta) d-abs d-local))
                (doseq [f (rest d)] (when-not (seq? f) 
                    (process-file (ext-instance f) (f :meta) 
                                                   (str d-abs-prefix   (f :base-name)) 
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
    (_try-apply-template file-struct local-prefix (file-meta-tpl (ext-instance file-struct))))
(defmethod try-apply-template true [file-struct local-prefix]
    (_try-apply-template file-struct local-prefix (dir-meta-tpl (ext-instance file-struct))))
    

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
