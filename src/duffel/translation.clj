(ns duffel.translation
    (:require [duffel.util :as dutil]))

(defn translate-dir
    "Given a directory name (should be prefixed with a _), returns the
    translation of it by environment variable, or the original name if
    unsuccessful"
    [dir-name]
    (let [ parsed-dir-name (apply str (rest dir-name)) ]
        (if-let [translated (System/getenv parsed-dir-name)]
            (dutil/remove-preceding-slash translated)
            dir-name)))
