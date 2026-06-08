export REALM=camunda
export BASE_URL=https://dev-mgmt.prg1paas.t-cloud.eu
export CLIENT_ID=camunda-identity-service
export CLIENT_SECRET=B6wXVJAf6U4Wc5crYXXlladXM7lXQQnV
export USERNAME=admin
export PASSWORD=BezpecneHeslo.123!

curl -s -X POST \
  "${BASE_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${CLIENT_ID}" \
  -d "client_secret=${CLIENT_SECRET}" \
  -d "grant_type=password" \
  -d "username=${USERNAME}" \
  -d "password=${PASSWORD}" \
  -d "scope=openid" \
  | jq -r '.access_token' > access_token.txt