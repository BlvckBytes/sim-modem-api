#!/bin/bash
sudo cp ensure_running.service /etc/systemd/system/rest_api_server_ensure_running.service
sudo systemctl enable rest_api_server_ensure_running.service
sudo systemctl start rest_api_server_ensure_running.service