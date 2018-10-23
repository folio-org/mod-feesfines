#!/bin/bash
token=`cat dikuToken`

curl -i -w '\n' -X POST -H 'Content-type: application/json' \
    -H 'X-Okapi-Tenant: diku' -H "X-Okapi-Token: $token" -d @transfer.json http://localhost:9130/owners

curl -i -w '\n' -X POST -H 'Content-type: application/json' \
    -H 'X-Okapi-Tenant: diku' -H "X-Okapi-Token: $token" -d @comment.json http://localhost:9130/feefines

