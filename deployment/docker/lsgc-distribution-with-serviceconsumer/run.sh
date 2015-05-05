#!/bin/bash
# this script runs the LSGC with the service-consumer inside
docker run -d --name="lsgc-serviceconsumer" -t lsgc/distribution-serviceconsumer
