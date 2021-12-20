package me.jameshunt.thefarm

import java.util.*

class PhotoManager {

    private val takePhotoTask = object : TimerTask() {
        override fun run() {
            takeAndroidPhoto()
        }
    }

    fun schedule(timer: Timer) {
        timer.schedule(takePhotoTask, 0, 1 * 60 * 1000L)
    }

    private fun takeAndroidPhoto() {
//    val isOn = "adb shell dumpsys input_method | grep screenOn".exec().contains("screenOn=true")
        val clickPowerButton = "adb shell input keyevent 26"

        val swipeUp = "adb shell input touchscreen swipe 930 880 930 380"
        val enterCode = "adb shell input text $phoneCode"
        val clickEnter = "adb shell input keyevent 66"
        val startApp = "adb shell am start -S me.jameshunt.remotecamera/me.jameshunt.remotecamera.MainActivity"

        clickPowerButton.exec()
        swipeUp.exec()
        enterCode.exec()
        clickEnter.exec()
        startApp.exec().log()
        Thread.sleep(10_000)
        clickPowerButton.exec()
    }
}