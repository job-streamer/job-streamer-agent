FROM dockerfile/java:oracle-java8
MAINTAINER kawasima <Yoshitaka Kawasima>

ENV LEIN_ROOT 1
RUN curl -L -s https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > \
    /usr/local/bin/lein \
  && chmod 0755 /usr/local/bin/lein \
  && lein upgrade

RUN mkdir -p /opt/job-streamer-agent
WORKDIR /opt/job-streamer-agent/
ADD project.clj ./
ADD lib lib
RUN lein with-profile docker deps
ADD . /opt/job-streamer-agent
CMD lein with-profile docker run
