(ns duffel.util)

(defn doall* 
    "Same as doall, but recursive"
    [s] (dorun (tree-seq seq? seq s)) s)

(defn index-when
    "Returns index of first item in seq for which (pred item) returns true"
    [pred seq]
    (loop [head (first seq)
           tail (rest seq)
           i    0]
        (when head
            (if (pred head)
                i
                (recur (first tail) (rest tail) (inc i))))))

(defn thread-until [init & fns]
    "Runs each fn on init until one of them doesn't return nil, and returns that result.
    Returns nil if none of them return anything but nil"
    (loop [headfn (first fns)
           tailfn (rest fns)]
        (when headfn
            (let [ret (headfn init)]
                (if (nil? ret) 
                    (recur (first tailfn) (rest tailfn)) 
                    ret)))))

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
