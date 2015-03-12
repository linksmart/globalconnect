FROM lsgc/base:latest
MAINTAINER Karl Catewicz "catewicz@fit.fraunhofer.de"
RUN mkdir -p ${LSGC_HOME}
ADD eu.linksmart.gc.distribution.main.deployable-0.0.1-20150311.144630-4-bin.tar.gz ${LSGC_HOME}
# redirect karaf logging to stdout
WORKDIR ${LSGC_HOME}/etc
RUN echo log4j.rootLogger=INFO, stdout, osgi:* >> org.ops4j.pax.logging.cfg
WORKDIR ${LSGC_HOME}
# create log file
RUN mkdir -p data/log
RUN echo >> data/log/karaf.log
#ENTRYPOINT ["./bin/karaf"]
EXPOSE 1099 8082 8181
#CMD echo "starting LSGC : " &&  ${LSGC_HOME}/bin/karaf server
CMD echo "starting karaf : " && ${LSGC_HOME}/bin/karaf server
