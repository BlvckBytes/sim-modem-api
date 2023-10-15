#!/bin/bash
while true
do
  /usr/bin/screen -S sim_modem_rest_api_server -Q select . 2>&1 > /dev/null

  # Non-zero exit code means that there was no such screen
  if [[ "`echo $?`" != "0" ]]
  then
    /home/blvckbytes/sim-modem-api/rest_api_server/start.sh
    echo "Started rest api server"
  fi

  echo sleeping
  sleep 5
done
