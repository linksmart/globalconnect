#!/bin/bash
SERVICES=
SERVICES=$(curl -s -H "accept:application/json" 'http://localhost:8082/NetworkManager?description="WeatherService"')
echo "services: $SERVICES"
VA=$(echo $SERVICES | jq .[0].VirtualAddress)
echo "virtual address: $VA"
SERVICE_UP=$(echo $VA | grep -e "0.0.0.[0-9]*" | wc -l)
echo "service up: $SERVICE_UP"
#SERVICE_UP=$(curl -s -H "accept:application/json" 'http://localhost:8082/NetworkManager?description="WeatherService"' | jq .[0].VirtualAddress | grep -e "0.0.0.[0-9]*" | wc -l)
if [[ "$SERVICE_UP" -gt 0 ]]
then
	echo "[OK] service up !!!"
	exit 0
else
	echo "[ERROR] service down!!!"
	exit 1
fi
