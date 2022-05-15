# clunky way to end previous process :/
pkill java & sleep 5 && nohup java -jar farmBrain-1.0-classifier.jar > $(date +"farm-%Y-%m-%dT%H:%M:%S%:z.log") 2>&1 &
