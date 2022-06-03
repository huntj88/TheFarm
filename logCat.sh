#!/bin/bash

# shellcheck disable=SC2010 # if 'ls | grep' breaks in this case its probably my fault making weird file names
# get last file with 'farm-' prefix, capture file name
MOST_RECENT_LOG_FILE=$(ls -1iq | grep farm- | tail -n1 | sed -n "s/^.*\(farm-\S*\).*$/\1/p")
cat "$MOST_RECENT_LOG_FILE"
