@echo off

pushd %0\..\..

set /p VERSION=<VERSION

java -cp dist\job-streamer-agent-%VERSION%.jar;"lib\*" clojure.main -m job-streamer.agent.core

pause

