#!/bin/bash

# Check for running server
if screen -ls | grep -q "No Sockets found in"; then
  echo "Could not find running server to stop"
  return -1 2>/dev/null || exit -1;
fi	

# Stop the server
screen -S casino -X quit
echo "Server Stopped"
