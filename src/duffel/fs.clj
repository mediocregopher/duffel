(ns duffel.fs
    (:require [duffel.ext  :as ext]
              [duffel.util :as util])
    (:import java.io.File))


(defn _tree [fo]
    (let [fo-ls (.listFiles fo)]
        (cons (.getName fo) 
            (->> (filter #(not (.isHidden %)) fo-ls)
                 (map #(if (.isDirectory %) (_tree %) (.getName %)))))))

(defn tree [dir]
    (_tree (File. dir)))

(def dot-underscore-split #"(.+)\._(.+)$")

(defn host-specifier-split [file-name]
    "Given a file name, returns a list where first item is
    the base filename (possibly with duffel extension), and the
    second is the host specifier (or '_default')"
    (if-let [re-res (re-find dot-underscore-split file-name)]
        (if (ext/is-extension (last re-res))
            (list file-name "_default")
            (rest re-res))
        (list file-name "_default")))

(defn extension-split [file-name]
    "Given a file name (assumes host specifier already split off)
    returns a list where first item is the base filename and the
    second is the extension to apply"
    (if-let [re-res (re-find dot-underscore-split file-name)]
        (if (ext/is-extension (last re-res))
            (rest re-res)
            (list file-name "put"))
        (list file-name "put")))

(def my-fdqn (.getCanonicalHostName (java.net.InetAddress/getLocalHost)))
(defn fdqn-matches [hostname]
    (= my-fdqn hostname))

(def my-hostname (.getHostName (java.net.InetAddress/getLocalHost)))
(defn hostname-matches [hostname]
    (= my-hostname hostname))

(defn index-of-specified-entry [host-seq]
    "Given a list of host specifiers, returns index of most specific one, or nil
    if none match"
    (util/thread-until host-seq
        (partial util/index-when fdqn-matches)
        (partial util/index-when hostname-matches)
        (partial util/index-when #(= "_default" %))))
