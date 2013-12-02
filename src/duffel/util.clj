(ns duffel.util)

(defn doall*
    "Same as doall, but recursive"
    [s] (dorun (tree-seq seq? seq s)) s)

(defn remove-trailing-slash
    "Given a string, if it ends in a slash, removes it"
    [dir-name]
    (if (= \/ (last dir-name))
        (apply str (butlast dir-name))
        dir-name))

(defn remove-preceding-slash
    "Given a string, if it begins with a slash, removes it"
    [dir-name]
    (if (= \/ (first dir-name))
        (apply str (rest dir-name))
        dir-name))

(defn append-slash [dir-name] (str dir-name "/"))

(defn path-split [path]
    (rest (re-find #"(.+?)([^\/]*)$" path)))

(defn str->int
    "Given a string, parses the first int out of it possible, or nil if none
    found"
    [s]
    (try (Integer/valueOf s) (catch Exception e nil)))

(defn full-octal
    "Given a string representing an octal (0644, 655, etc...), if the octal only
    has three numbers instead of four, prepends a zero"
    [octal-str]
    (if (not (= (count octal-str) 4))
        (str "0" octal-str)
        octal-str))
