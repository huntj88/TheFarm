#!/bin/bash
adb start-server &&
adb shell input keyevent 26 &&
adb shell input touchscreen swipe 930 880 930 380 &&
adb shell input text 6463 &&
adb shell input keyevent 66 &&
adb shell "am start -a android.media.action.IMAGE_CAPTURE" && # resets any camera resource locks
sleep 3
adb shell am start -S -n me.jameshunt.remotecamera/.MainActivity &&
sleep 10 &&
adb shell input keyevent 26