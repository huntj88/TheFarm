#!/bin/bash
ifconfig wlan0 down &&
sleep 30 &&
ifconfig wlan0 up