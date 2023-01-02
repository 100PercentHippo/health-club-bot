#!/bin/bash

# Check for running server
if screen -ls | grep -q "casino"; then
  echo "Server already running"
  return -1 2>/dev/null || exit -1;
fi

# Fetch API_KEY from secret store
if [ -z "$API_KEY" ]; then
  echo "API_KEY unset or empty, fetching..."
  export API_KEY=$(curl "https://secretmanager.googleapis.com/v1/projects/norse-sequence-373221/secrets/API_KEY/versions/1:access" \
    --request "GET" \
    --header "authorization: Bearer $(gcloud auth print-access-token)" \
    --header "content-type: application/json" | jq -r .payload.data | base64 -d)
fi

# Configure JDBC environment variables
if [ -z "$JDBC_PASSWORD" ]; then
  echo "JDBC_PASSWORD unset or empty, fetching..."
  export JDBC_DATABASE_URL="jdbc:postgresql://35.233.156.255:5432/postgres"
  export JDBC_USERNAME=postgres
  export JDBC_PASSWORD=$(curl "https://secretmanager.googleapis.com/v1/projects/norse-sequence-373221/secrets/DB_PASSWORD/versions/1:access" \
    --request "GET" \
    --header "authorization: Bearer $(gcloud auth print-access-token)" \
    --header "content-type: application/json" | jq -r .payload.data | base64 -d)
fi

# Run the server
cd /home/kevin/health-club-bot/
screen -dmS casino bash -c "java -cp target/classes/:target/dependency/* com.c2t2s.hb.HBMain $API_KEY > logs/current.log"
echo "Server started successfully"
