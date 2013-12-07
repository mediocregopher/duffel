(ns duffel.ext.core)

(defprotocol duffel-extension
    "Protocol which defines an extension which can be used by duffel"

    (process-dir [x app dtree]
        "Called on a dtree with this extension. Should return the new dtree that
        you want processing to continue on (the parent is processed before any
        children. Side-effects should also go here")

    (process-file [x app file-map]
        "called on a file-map with this extension"))
