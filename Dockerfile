FROM dockerfile/java:oracle-java8

ENV LEIN_ROOT 1
RUN curl -L -s https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > \
    /usr/local/bin/lein \
  && chmod 0755 /usr/local/bin/lein \
  && lein upgrade

RUN git clone https://github.com/job-streamer/job-streamer-agent.git
WORKDIR job-streamer-agent
RUN lein deps
CMD lein run
