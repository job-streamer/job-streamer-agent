(defproject net.unit8.job-streamer/job-streamer-agent "0.2.0-SNAPSHOT"
  :dependencies [[javax/javaee-api "7.0" :scope "provided"]
                 [org.jberet/jberet-core "1.1.0.Final"]
                 [org.jberet/jberet-se "1.1.0.Final"]
                 [org.jboss.marshalling/jboss-marshalling "1.4.7.Final"]
                 [org.jboss.logging/jboss-logging "3.3.0.Final"]
                 
                 [org.jboss.weld/weld-core "2.2.13.Final"]
                 [org.jboss.weld.se/weld-se "2.2.13.Final"]
                 [net.unit8.weld/weld-prescan "0.1.0-SNAPSHOT"]
                 
                 [org.wildfly.security/wildfly-security-manager "1.1.2.Final"]
                 [com.google.guava/guava "18.0"]
                 [com.h2database/h2 "1.4.187"]

                 [org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [environ "1.0.0"]
                 [net.unit8.wscl/websocket-classloader "0.2.1"]
                 [net.unit8.logback/logback-websocket-appender "0.1.0"]
                 [io.undertow/undertow-websockets-jsr "1.1.1.Final"]
                 [liberator "0.13"]
                 [http-kit "2.1.19"]
                 [compojure "1.3.4"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-devel "1.3.2"]
                 [ch.qos.logback/logback-classic "1.1.3"]]
  :plugins [[lein-libdir "0.1.1"]]
  :libdir-path "lib"
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :main job-streamer.agent.core
  :aot :all  
  :profiles {:docker {:local-repo "lib"}})
