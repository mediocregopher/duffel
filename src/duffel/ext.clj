(ns duffel.ext)

(def extensions [ "put" "tpl" ])

(defn is-extension [suffix]
    (not (nil? (some #{suffix} extensions))))
