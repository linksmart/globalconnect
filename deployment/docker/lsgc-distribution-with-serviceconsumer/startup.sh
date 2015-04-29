#!/bin/bash
# this script is started inside the docker image. 
# 1. it starts LSGC
# 2. waits
# 3. calls the remote REST weather service
echo "starting LSGC node..."
./bin/start
echo "sleeping 65 sec..."
sleep 65
echo "wake up!"
echo "calling remote REST service..."
./bin/getTemperature.sh
