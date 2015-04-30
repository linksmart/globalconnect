#!/bin/bash
# this script is started inside the docker image. 
# 1. it starts LSGC
# 2. waits
# 3. calls the remote REST weather service
TIMER=300
## lower valus may not work due the ineffictient discovery mechanism
while :
do
	echo "(re)starting LSGC instance..."
	./bin/start
	echo "sleeping $TIMER sec..."
	sleep $TIMER
	echo "checking remote service availability..."
	./bin/isServiceUp.sh
	if [ $? -eq 0 ]; then
		echo "remote service is running. leaving the loop."
		./bin/printLogs.sh
		break
	fi
	./bin/printLogs.sh
	echo "stopping LSGC instance..."
	./bin/stop
	sleep 10
	echo "done."
done
sleep 5
echo "calling remote REST service..."
./bin/getTemperature.sh
