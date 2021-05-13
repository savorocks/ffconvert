#!/usr/bin/bb

(require '[babashka.deps :as deps])
(require '[clojure.string :as st])
(require '[clojure.java.shell :refer [sh]])

(deps/add-deps '{:deps {ffclj/ffclj {:mvn/version "0.1.2" }}})

(require '[ffclj.core :refer [ffmpeg! ffprobe!]]
         '[ffclj.task :as task])

(def cwd "/home/savo/fftest/")
(def dest (str cwd "lame/"))

(defn get-enc [file]
  (:encoder (:tags (first (:streams (ffprobe! [:show_streams (str cwd file)]))))))
(defn get-bitrate [file]
  (subs (:bit_rate (first (:streams (ffprobe! [:show_streams (str cwd file)])))) 0 3))

(defn rem-ext [file]
  (st/replace file #".mp3|.m4a" ""))

(defn get-files [dir]
  (filter (fn [x] (or (st/includes? x ".mp3") (st/includes? x ".m4a")))
          (st/split-lines (:out (sh "ls" dir)))))

(defn mkdir []
  (if (= 2 (:exit (sh "ls" dest)))
    (sh "mkdir" dest)
    (:err (sh "mkdir" dest))))

;;(def filelist (clojure.set/difference (set (get-files cwd)) (set (get-files dest))))
(def filelist (get-files cwd))
(defn get-non-lame-files [flist]
  (filter (fn [x] (not (st/includes? (st/lower-case (str (get-enc x))) "lame"))) flist))

(defn convert [file]
  (do
    (with-open [task (ffmpeg! [:y
                                :i (str cwd file)
                                        ;:b (get-bitrate "audacity.mp3")
                                :f "wav"
                                ;;[:acodec "libmp3lame"]
                                (str dest (rem-ext file))])]
       (.wait-for task)
       (.stdout task)
       ;;(println "Conversion of:" file "completed. Exit code:" (.exit-code task))
        )
      (let [bitrate (get-bitrate file)
            input (str dest (rem-ext file))
            output (str input ".mp3")]
        (sh "lame" "--quiet"  input "-b" bitrate output)
        (sh "rm" input))))

;; use (map convert filelist) and first do (mkdir) all will be right.

;;(map convert filelist)
;;(keys (group-by get-enc filelist))
(mkdir)
(map convert (get-non-lame-files filelist))
