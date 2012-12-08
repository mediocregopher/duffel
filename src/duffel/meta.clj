(ns duffel.meta
   (:import java.lang.System))

(def default-username (java.lang.System/getProperty "user.name"))
(def default-group default-username)    ;Assume primary group = username, cause fuck it

(def file-meta-tpl { :chmod (list :string (list :optional "0644") '(:regex #"[0-7]{3,4}"))
                     :owner (list :string (list :optional default-username)             )
                     :group (list :string (list :optional default-group)                ) })

(def dir-meta-tpl  (merge file-meta-tpl
                   { :apply_to_contents '(:bool (:optional false))
                     :apply_recursively '(:bool (:optional false))
                     :delete_untracked  '(:bool (:optional false)) }))
