#!/bin/bash
token=`cat dikuToken`

curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/owners
# /feefines Require ownerId
curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/feefines
curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/accounts
curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/transfer-criterias
curl -D - -w '\n' -H "X-Okapi-Tenant: diku" -H "X-Okapi-Token: $token" http://localhost:9130/overdue-fines-policies
