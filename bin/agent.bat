@echo off

pushd %0\..\..

set /p VERSION=<VERSION

java -cp %AGENT_RESOURCE_PATH%;dist\job-streamer-agent-%VERSION%.jar;"lib\*" clojure.main -m job-streamer.agent.main

pause
