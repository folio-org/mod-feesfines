#!/bin/bash
token=`cat dikuToken`

curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/owners
curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/feefines
# /accounts Require add 
curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/accounts
#Depend to accounts
curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/feefinehistory
curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/chargeitem
