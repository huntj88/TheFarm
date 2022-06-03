#!/bin/bash
scp setupDockerPi.sh setupSystemd.sh install.sh startFarm.sh logTail.sh logCat.sh brain/takePicture.sh brain/build/libs/farmBrain-1.0-classifier.jar pi@192.168.1.83:/home/pi/TheFarm &&
echo "copied scripts to pi"
