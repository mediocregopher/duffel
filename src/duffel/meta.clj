(ns duffel.meta
    (:require [cheshire.core :refer :all])
    (:use massage.json)
    (:import java.lang.System))

(def default-username (java.lang.System/getProperty "user.name"))
(def default-group default-username)    ;Assume primary group = username, cause fuck it

(def file-meta-tpl { :chmod (list :string (list :optional "0644") '(:regex #"[0-7]{3,4}"))
                     :owner (list :string (list :optional default-username)             )
                     :group (list :string (list :optional default-group)                ) })

(def dir-meta-tpl  (merge file-meta-tpl
                   { :apply_to_contents '(:bool (:optional false))
                     :apply_recursively '(:bool (:optional false))
                     :delete_untracked  '(:bool (:optional false)) }))

(defn try-json
    "Given a string, tries to parse it as a json map. Returns nil if failure"
    [json-string]
    (let [parse-result (try (parse-string json-string true) (catch java.lang.Exception e nil))]
        (if (map? parse-result) parse-result nil)))

(defn string-key
    "Given a map under construction and a key/value pair, adds the key/value pair to the map
    but with the key as a string if it was a keyword previously"
    [json-map keyval]
    (let [oldk (first keyval)
          oldv (val keyval)
          k    (if (string? oldk) oldk (name oldk)) ]
        (assoc json-map k oldv)))

(defn parse-meta-string
    "Given a string (presumably from a _meta.json file) parses it as best as possible as
    into a metadata object. All top level keys are turned into strings. The values of those
    keys are NOT massaged"
    [meta-json]
    (when-let [json-struct (try-json meta-json)]
        (reduce string-key {} json-struct)))

(defn tpl-put-file [meta-struct] 
    (parse-json meta-struct file-meta-tpl))

(defn tpl-put-dir [meta-struct] 
    (parse-json meta-struct dir-meta-tpl))
