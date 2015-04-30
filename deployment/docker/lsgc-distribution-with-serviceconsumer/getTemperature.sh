#!/bin/bash
# this script calls the network manager and later the found, remote WeatherService servlet
VA=$(curl -s -H "accept:application/json" 'http://localhost:8082/NetworkManager?description="WeatherService"' | jq .[0].VirtualAddress)
VA=$(echo $VA | sed "s/\"//g")
echo "parsed VAD: $VA"
RAW_RESPONSE=$(curl http://localhost:8082/HttpTunneling/0/$VA/sensor/1)
echo "raw response: $RAW_RESPONSE"
TEMPERATURE=$(echo $RAW_RESPONSE | jq .Temperature)
echo "parsed temperature: $TEMPERATURE"
if [[ "$TEMPERATURE" == "25" ]]; then
	echo "[OK] valid temperature found"
	./bin/stop
	exit 0
else
	echo "[ERROR] wrong temperature"
	./bin/stop
	exit 1
fi
