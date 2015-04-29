#!/bin/bash
# this script calls the network manager and later the found, remote WeatherService servlet
VA=$(curl -s -H "accept:application/json" 'http://localhost:8082/NetworkManager?description="WeatherService"' | jq .[0].VirtualAddress)
echo "parsed VAD: $VA"
TEMPERATURE=$(curl http://localhost:8082/HttpTunneling/0/$VA/sensor/1 | jq .Temperature)
echo "parsed temperature: $TEMPERATURE"
if [[ "$TEMPERATURE" == "25" ]]; then
	echo "[OK] valid temperature found"
	exit 0
else
	echo "[ERROR] wrong temperature"
	exit 1
fi
