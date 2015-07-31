#!/bin/bash
# this script is started inside the docker image. 
# 1. it starts LSGC
# 2. waits
# 3. calls the remote REST weather service
TIMER=90
## lower valus may not work due the ineffictient discovery mechanism
echo "---> starting LSGC instance with consumer..."
./bin/start
echo "---> script sleeps $TIMER sec before consuming service ..."
sleep $TIMER
echo "---> calling remote REST service..."
./bin/getTemperature.sh
STATUS_CODE=$?
echo "---> service responded : $STATUS_CODE"
echo "---> printing karaf logs..."
./bin/printLogs.sh
echo "---> stopping LSGC instance..."
./bin/stop
echo "---> done."
# return exitcode for further processing (jenkins)
exit $STATUS_CODE
