#!/bin/bash
# this script is started inside the docker image. 
# 1. it starts LSGC
# 2. waits and prints karaf logs
LOG_TIMER=60
echo "---> starting LSGC instance with weather servlet..."
./bin/start
echo "---> done."
echo "---> LSGC instance runs for $LOG_TIMER sec..."
sleep $LOG_TIMER
echo "---> printing karaf logs..."
./bin/printLogs.sh
echo "---> stopping LSGC instance..."
./bin/stop
echo "---> done."
