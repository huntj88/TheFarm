# run with sudo
# create docker group so we don't need to use sudo in the future
echo "adding group"
groupadd docker || true
echo "adding current user to group"
usermod -aG docker $USER || true
echo "refreshing group evaluation"
newgrp docker
echo "exiting"
