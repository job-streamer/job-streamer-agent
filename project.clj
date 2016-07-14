(defproject net.unit8.job-streamer/job-streamer-agent (clojure.string/trim-newline (slurp "VERSION"))
  :dependencies [[javax/javaee-api "7.0"]
                 [org.jberet/jberet-core "1.2.0.Final"]
                 [org.jberet/jberet-se "1.2.0.Final"]
                 [org.jboss.marshalling/jboss-marshalling "1.4.10.Final"]
                 [org.jboss.logging/jboss-logging "3.3.0.Final"]

                 [org.jboss.weld/weld-core "2.2.16.Final"]
                 [org.jboss.weld.se/weld-se "2.2.16.Final"]
                 [net.unit8.weld/weld-prescan "0.1.0"]

                 [org.wildfly.security/wildfly-security-manager "1.1.2.Final"]
                 [com.google.guava/guava "19.0"]
                 [com.h2database/h2 "1.4.192"]

                 [org.clojure/clojure "1.8.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [duct "0.8.0"]
                 [meta-merge "1.0.0"]

                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/core.async "0.2.374"]
                 [environ "1.0.3"]
                 [net.unit8.wscl/websocket-classloader "0.2.1"]
                 [net.unit8.logback/logback-websocket-appender "0.1.0"]
                 [io.undertow/undertow-websockets-jsr "1.3.22.Final"]
                 [liberator "0.14.1"]
                 [http-kit "2.1.19"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-devel "1.5.0"]
                 [ch.qos.logback/logback-classic "1.1.7"]]
  :source-paths ["src/clj"]
  :test-paths   ["test/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]
  :prep-tasks [["javac"] ["compile"]]

  :main ^:skip-aot job-streamer.agent.main

  :pom-plugins [[org.apache.maven.plugins/maven-assembly-plugin "2.5.5"
                 {:configuration [:descriptors [:descriptor "src/assembly/dist.xml"]]}]]

  :profiles
  {:dev    [:project/dev  :profiles/dev]
   :test   [:project/test :profiles/test]
   :docker [:project/docker :profiles/docker]
   :uberjar {:aot :all}
   :profiles/dev    {}
   :profiles/test   {}
   :profiles/docker {}
   :project/dev   {:dependencies [[duct/generate "0.8.0"]
                                  [reloaded.repl "0.2.2"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [eftest "0.1.1"]
                                  [kerodon "0.8.0"]]
                   :source-paths ["dev"]
                   :jvm-opts ["-Dwscl.cache.directory=${user.home}/.wscl-cache"]
                   :repl-options {:init-ns user}
                   :env {}}
   :project/test {:dependencies [[junit "4.12"]]}
   :project/docker {:local-repo "lib"}})
