curl -w '\n' -X POST -D - \
  -H "Content-type: application/json" \
  -H "X-Okapi-Tenant: diku" \
  -d @okapi-login.json \
  http://localhost:9130/authn/login
