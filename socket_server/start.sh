#!/bin/bash
echo "19d2 0016" | sudo tee -a /sys/bus/usb-serial/drivers/option1/new_id
sleep .5
basepath=/home/blvckbytes/sim-modem-api/socket_server
/usr/bin/screen -m -d -S sim_modem_socket_server -L -Logfile $basepath/`date +"%Y-%m-%d-%H-%M-%S-%N"`.txt $basepath/socket_server