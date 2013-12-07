(ns duffel.ext.impl.ignore
    (:require [duffel.ext.core :refer [duffel-extension]]))

(deftype ignore-ext [] duffel-extension

    ; We don't want to process anything inside an ignored directory
    (process-dir [x app dtree] (list (first dtree)))

    (process-file [x app file-map] nil)
)
