(ns duffel.util)

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
