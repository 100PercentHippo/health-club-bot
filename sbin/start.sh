#!/bin/bash

# Check for running server
if screen -ls | grep -q "casino"; then
  echo "Server already running"
  return -1 2>/dev/null || exit -1;
fi

# Ensure API Token is set
if [ -z "$BOT_TOKEN" ]; then
  echo "No API Token set. Should be set via the environment variable BOT_TOKEN"
  return -1 2>/dev/null || exit -1;
fi

# Run the server
datetime=$(date +"%Y%m%d-%H%M%S")
screen -dmS casino bash -c "java -cp target/classes/:target/dependency/* com.c2t2s.hb.HBMain $BOT_TOKEN > logs/log-$datetime.log"
if [ -e logs/log-current.log ]
then
  rm logs/log-current.log
fi
ln -s "logs/log-$datetime.log" logs/log-current.log

echo "Server started successfully"
