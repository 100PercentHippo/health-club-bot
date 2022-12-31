#!/bin/bash

# Fetch API_KEY from secret store
export API_KEY=$(curl "https://secretmanager.googleapis.com/v1/projects/norse-sequence-373221/secrets/API_KEY/versions/1:access" \
    --request "GET" \
    --header "authorization: Bearer $(gcloud auth print-access-token)" \
    --header "content-type: application/json" | jq -r .payload.data | base64 -d)

# Run the server
java -cp target/classes/:target/dependency/* com.c2t2s.hb.HBMain $API_KEY