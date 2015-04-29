#!/bin/bash
# this script evaluates  the log from the finished consumer docker container
OK_LOG=$(sudo docker logs lsgc-serviceconsumer | grep '\[OK\] valid temperature found' | wc -w)
if [[ $OK_LOG == "4" ]]; then
	echo "[OK] proper log from container found"
	exit 0
else
	echo "[ERROR] wrong log from container found"
fi

