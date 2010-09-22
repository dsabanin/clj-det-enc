(ns
  ^{:author "Takahiro Hozumi",
    :doc "A detect encoding utility. juniversalchardet wrapper."}
  detect-encoding-clj.core
  (:require [clojure.java.io :as io :only [input-stream]])
  (:import [org.mozilla.universalchardet UniversalDetector]
	   [java.nio.charset Charset]))

(defn- judge-seq! [buf istream detector]
  (lazy-seq
   (let [n        (.read istream buf)
	 proceed? (and (> n 0)
		       (not (do (.handleData detector buf 0 n)
				(.isDone detector))))]
     (if proceed?
       (cons proceed?
	     (judge-seq! buf istream detector))
       (cons proceed? ())))))

(defn detect
  "Attempts to detect the encoding of a text by using juniversalchardet java library.
   
   Usage: (detect target)

   (detect \"unknownA.txt\")
   => \"UTF-8\"
   (detect \"unknownB.txt\")
   => nil

   Usage: (detect target encodingname-when-unknown)

   (detect \"unknownB.txt\" \"EUC-JP\")
   => \"EUC-JP\"
   (detect \"unknownB.txt\" :default)
   => \"SHIFT_JIS\"

   return : encoding name or nil when target encoding cannot be detected.
   target : Whatever clojure.java.io/input-stream can deal with.
            (File, filename(String), InputStream, BufferedStream etc)
            Target stream is closed automatically.
   encodingname-when-unknown :
            Return this value when target encoding cannot be detected.
            :default means the default charset of this Java virtual machine.

   What encodings can be detected?
   See http://code.google.com/p/juniversalchardet/"
  ([target]
     (detect target nil))
  ([target encodingname-when-unknown]
     (let [buf       (make-array Byte/TYPE 4096)
	   detector  (UniversalDetector. nil)
	   istream   (io/input-stream target)]
       (try
	 (dorun (judge-seq! buf istream detector))
	 (.dataEnd detector)
	 (or (.getDetectedCharset detector)
	     (if (= encodingname-when-unknown :default)
	       (.displayName (Charset/defaultCharset))
	       encodingname-when-unknown))
	 (finally
	  (.close istream)
	  (.reset detector))))))
