(import [java.nio.file Paths FileSystems StandardWatchEventKinds])

(require '[clojure.java.io :as io]
         '[clojure.string :as string])

(defn ^:private watch-dir [list-dir callback]
  (let [service (.newWatchService (FileSystems/getDefault))]
    (doseq [dir list-dir]
      (.register
       (Paths/get dir (into-array String []))
       service
       (into-array java.nio.file.WatchEvent$Kind [StandardWatchEventKinds/ENTRY_MODIFY])))
    (loop []
      (let [key (.take service)]
        (doseq [event (.pollEvents key)]
          (when (= (.kind event) StandardWatchEventKinds/ENTRY_MODIFY)
            (callback (.context event))))
        (when (.reset key)
          (recur))))))

(defn ^:private get-target-files []
  (->> (file-seq (io/as-file "."))
       (filter #(and
                 (.isFile %)
                 (.endsWith (.getName %) ".clj")))
       (map str)))

(defn ^:private clear-repl []
  (println "\033[H\033[2J")
  (flush))

(defn ^:private get-file-dir [file]
  (->>
   (string/split file #"\/")
   (drop-last)
   (string/join "/")))

(defn ^:private get-file-dirs [files]
  (set (map get-file-dir files)))

(defn ^:private path-size [path]
  (.lastModified (.toFile path)))

(future
  (watch-dir (get-file-dirs (get-target-files))
             (fn [path]
               (when (> (path-size path) 0)
                 (clear-repl)
                 (load-file (str (.toAbsolutePath path))))
               (Thread/sleep 50))))
(clear-repl)
