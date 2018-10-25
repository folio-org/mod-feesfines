#!/bin/bash


curl -i -w '\n' -X GET http://localhost:9130/_/proxy/tenants

#**********************************Declare the module to Okapi
curl -w '\n' -X POST -D -   \
    -H "Content-type: application/json"   \
    -d @../target/ModuleDescriptor.json \
    http://localhost:9130/_/proxy/modules

curl -i -w '\n' -X GET http://localhost:9130/_/proxy/modules
curl -i -w '\n' -X GET http://localhost:9130/_/proxy/modules/mod-feesfines-15.0.2-SNAPSHOT

#**********************************Deploying the module
curl -w '\n' -D - -s \
   -X POST \
   -H "Content-type: application/json" \
   -d @../target/DeploymentDescriptor.json  \
   http://localhost:9130/_/discovery/modules

#**********************************Enable the module for our tenant:

curl -i -w '\n' \
   -X POST \
   -H "Content-type: application/json" \
   -d @okapi-enable-feefines.json \
   http://localhost:9130/_/proxy/tenants/diku/modules


