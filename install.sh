#!/bin/bash
# run with sudo

# TODO: set raspberry time to local time?

echo "installing git"
apt install -y git

echo "Installing java"
apt install -y default-jdk

echo "Installing adb"
apt-get install -y android-tools-adb

if [ -x "$(command -v docker)" ]; then
  echo "Docker installed already"
else
  echo "Installing docker"
  curl -fsSL https://get.docker.com -o get-docker.sh
  sh get-docker.sh
fi

echo "configuring docker"
source setupDockerPi.sh # seems to not work?

echo "Creating System Service"
source setupSystemd.sh

echo "install complete, rebooting"
reboot
