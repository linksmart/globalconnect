FROM lsgc/distribution:latest
MAINTAINER Karl Catewicz "catewicz@fit.fraunhofer.de"
# 
# inject client code into the LSGC docker container
#
ADD getTemperature.sh ${LSGC_HOME}/bin/
RUN chmod a+x ${LSGC_HOME}/bin/getTemperature.sh
ADD startup.sh ${LSGC_HOME}/bin/
RUN chmod a+x ${LSGC_HOME}/bin/startup.sh
ADD printLogs.sh ${LSGC_HOME}/bin/
RUN chmod a+x ${LSGC_HOME}/bin/printLogs.sh
#
# start karaf and call service over tunnel
#
CMD ["/bin/bash", "/opt/lsgc/bin/startup.sh"]




