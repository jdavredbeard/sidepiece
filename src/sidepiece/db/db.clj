(ns sidepiece.db.db
  (:require [sidepiece.db.schema :as schema]
            [datomic.client.api :as d]))

(defn transact
  [conn schema] (d/transact conn {:tx-data schema}))

(defn transact-all
  "Load and run all transactions from schema-seq."
  [conn schema-seq]
  (loop [n 0
         [tx & more] schema-seq]
    (if tx
      (recur (+ n (count (:tx-data (d/transact conn {:tx-data tx}))))
             more)
      {:datoms n})))

(defn init-db
  "creates db client, deletes previous db state, creates new db, creates connection, transacts initial schema, returns connection"
  []
  (let [client (d/client {:server-type :dev-local
                          :system "dev"})]
    (d/delete-database client {:db-name "musicians"})
    (d/create-database client {:db-name "musicians"})
    (let [conn (d/connect client {:db-name "musicians"})]
      (transact-all conn schema/init-schema)
      (transact-all conn schema/init-musicians)
      conn)))


(def conn (init-db))

(defn db
  []
  (d/db conn))