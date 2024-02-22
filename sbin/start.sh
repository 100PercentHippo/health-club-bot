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

# Check if we should initialize commands
if [ "$#" -gt 0 ]
then
  if [ "$1" == "init" ]
  then
    init=true
  else
    init=
  fi
fi

# Run the server
datetime=$(date +"%Y%m%d-%H%M%S")
if [ -n "$init" ]
then
  screen -dmS casino bash -c "java -cp target/classes/:target/dependency/* com.c2t2s.hb.HBMain $BOT_TOKEN init > logs/log-$datetime.log 2>&1"
else
  screen -dmS casino bash -c "java -cp target/classes/:target/dependency/* com.c2t2s.hb.HBMain $BOT_TOKEN > logs/log-$datetime.log 2>&1"
fi

if [ -e logs/log-current.log ]
then
  rm logs/log-current.log
fi
cd logs
ln -s log-$datetime.log log-current.log
cd ..

echo "Server started successfully"
