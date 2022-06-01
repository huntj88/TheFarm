#!/bin/bash

# cd to path of this file
parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" || exit 1 ; pwd -P )
cd "$parent_path" || exit 1

stopFarm() {
  echo "Caught SIGTERM signal!"
  kill -TERM "$child" 2>/dev/null
}
# make sure SIGTERM sent to java process
trap stopFarm SIGTERM

# actual command to start java process and send logs to dated file
java -jar farmBrain-1.0-classifier.jar > $(date +"farm-%Y-%m-%dT%H:%M:%S%:z.log") 2>&1 &

child=$!
wait "$child"
