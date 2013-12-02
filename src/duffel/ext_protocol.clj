(ns duffel.ext-protocol)

(defprotocol duffel-extension
    "Protocol which defines an extension which can be used by duffel"

    (preprocess-dir [x dir-tree]
        "Called on a dir-tree for doing transformations before the metadata
        templates get applied. Should return the transformed dir-tree. Shouldn't
        have side-effects")

    (preprocess-file [x file-struct]
        "Called on a file-struct for doing transformations before the metadata
        templates get applied. Should return the transformed file-struct.
        Shouldn't have side-effects")

    (dir-meta-tpl [x]
        "Returns the massage template which will be applied to the metadata for
        dirs")

    (file-meta-tpl [x]
        "Returns the massage template which will be applied to the metadata for
        files")

    (postprocess-dir [x dir-struct]
        "Called on a dir-struct (NOT dir-tree) after the massage templates are
        applied, before actual processing happens")

    (postprocess-file [x file-struct]
        "Called on a file-struct after the massage templates are applied, before
        actual processing happens")

    (process-dir [x app meta-struct abs local]
        "Called for dir to do actual processing. This is where all the
        side-effects go")

    (process-file [x app meta-struct abs local]
        "Called for a file to do actual processing. This is where all the
        side-effects go"))
