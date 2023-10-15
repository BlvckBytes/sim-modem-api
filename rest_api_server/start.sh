#!/bin/bash
basepath=/home/blvckbytes/sim-modem-api/rest_api_server
/usr/bin/screen -m -d -S sim_modem_rest_api_server -L -Logfile $basepath/`date +"%Y-%m-%d-%H-%M-%S-%N"`.txt java -jar $basepath/server.jar