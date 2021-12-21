package me.jameshunt.thefarm

import java.util.*

class PhotoManager {

    private val takePhotoTask = object : TimerTask() {
        override fun run() {
            val runId = 1 // TODO: configurable
            takeAndroidPhoto(runId)
        }
    }

    fun schedule(timer: Timer) {
        timer.schedule(takePhotoTask, 0, 1 * 60 * 1000L)
    }

    private fun takeAndroidPhoto(runId: Int) {
        val clickPowerButton = "adb shell input keyevent 26"

        val swipeUp = "adb shell input touchscreen swipe 930 880 930 380"
        val enterCode = "adb shell input text $phoneCode"
        val clickEnter = "adb shell input keyevent 66"
        val startApp = "adb shell am start -S -n me.jameshunt.remotecamera/.MainActivity --ei runId $runId"

        clickPowerButton.exec()
        swipeUp.exec()
        enterCode.exec()
        clickEnter.exec()
        startApp.exec().log()
        Thread.sleep(10_000)
        clickPowerButton.exec()
    }
}