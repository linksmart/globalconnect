#!/bin/bash
# this script builds the LSGC with the weather service inside
docker build -rm -t lsgc/distribution-weatherservice .
