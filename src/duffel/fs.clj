(ns duffel.fs
    (:import java.io.File))


(defn _tree [fo]
    (let [fo-ls (.listFiles fo)]
        (cons (.getName fo) 
            (map
                #(if (.isDirectory %) (_tree %) (.getName %))
            (filter 
                #(not (.isHidden %)) fo-ls))))) 

(defn tree [dir]
    (_tree (File. dir)))
