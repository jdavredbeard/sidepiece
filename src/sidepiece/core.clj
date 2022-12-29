(ns sidepiece.core
  (:require [sidepiece.db.handlers :as h]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (h/add-init-bookings))
