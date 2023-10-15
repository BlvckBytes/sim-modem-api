#!/bin/bash
while true
do
  /usr/bin/screen -S sim_modem_socket_server -Q select . 2>&1 > /dev/null

  # Non-zero exit code means that there was no such screen
  if [[ "`echo $?`" != "0" ]]
  then
    /home/blvckbytes/sim-modem-api/socket_server/start.sh
    echo "Started socket server"
  fi

  echo sleeping
  sleep 5
done
