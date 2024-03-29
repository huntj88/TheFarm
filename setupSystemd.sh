#!/bin/bash

echo "Creating new systemd service"
cat > /lib/systemd/system/thefarm.service <<- EOM
[Unit]
Description=Farming software

Wants=network.target
After=syslog.target network-online.target

[Service]
Type=simple
ExecStart=/bin/bash /home/pi/TheFarm/startFarm.sh
Restart=on-failure
RestartSec=10
KillMode=control-group

[Install]
WantedBy=multi-user.target
EOM

echo "Enabling new systemd service"
systemctl enable thefarm