#!/bin/bash
sudo cp ensure_running.service /etc/systemd/system/
sudo systemctl enable ensure_running.service
sudo systemctl start ensure_running.service