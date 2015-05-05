#!/bin/bash
# this script is started inside the docker image. 
# 1. it starts LSGC
# 2. waits and prints karaf logs
#TIMER=30
echo "---> starting LSGC instance with weather servlet..."
./bin/start
echo "---> main startup script sleeps $TIMER sec..."
sleep $TIMER
echo "---> printing karaf logs..."
./bin/printLogs.sh
echo "---> stopping LSGC instance..."
./bin/stop
echo "---> done."
