#!/bin/bash
# this script runs the LSGC with the weather service inside. internal jetty port is accesible at 8882
sudo docker run  -d -p 1099:1099 -p 8882:8882 -p 8181:8181 --name="lsgc-weatherservice" lsgc/distribution-weatherservice
