#!/bin/bash

# Only execute if running on Pi
if [ -d "/home/pi/TheFarm" ]; then
  echo "Stopping docker"
  systemctl stop docker

  echo "Deleting all containers, images, network, volumes, runtimes, tmp, etc"
  rm -rf /var/lib/docker/*

  echo "Starting docker"
  systemctl start docker
fi