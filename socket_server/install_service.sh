#!/bin/bash
sudo cp ensure_running.service /etc/systemd/system/socket_server_ensure_running.service
sudo systemctl enable socket_server_ensure_running.service
sudo systemctl start socket_server_ensure_running.service