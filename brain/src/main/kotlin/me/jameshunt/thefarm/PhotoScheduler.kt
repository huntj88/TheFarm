package me.jameshunt.thefarm

import me.jameshunt.brain.sql.LogQueries
import java.util.*

class PhotoScheduler(private val logQueries: LogQueries) {
    private val androidPhoneIp = "192.168.1.73" // TODO: configurable

    private val takePhotoTask = object : TimerTask() {
        override fun run() {
            val runId = 1 // TODO: configurable
            try {
                takeAndroidPhoto(runId)
            } catch (e: Exception) {
                logQueries.insert(LogLevel.Error, "PHOTO", "error taking a picture: $e")
            }
        }
    }

    fun schedule(timer: Timer) {
        timer.schedule(takePhotoTask, 0, 10 * 60 * 1000L)
    }

    private fun takeAndroidPhoto(runId: Int) {
        val adbConnect = "adb connect $androidPhoneIp"

        val clickPowerButton = "adb shell input keyevent 26"
        val swipeUp = "adb shell input touchscreen swipe 930 880 930 380"
        val enterCode = "adb shell input text $phoneCode"
        val clickEnter = "adb shell input keyevent 66"
        val startApp = "adb shell am start -S -n me.jameshunt.remotecamera/.MainActivity --ei runId $runId"

        adbConnect.exec().also {
            logQueries.insert(LogLevel.Info, "PHOTO", it)
        }
        clickPowerButton.exec()
        swipeUp.exec()
        enterCode.exec()
        clickEnter.exec()
        startApp.exec().also {
            logQueries.insert(LogLevel.Info, "PHOTO", "taking a picture")
        }
        Thread.sleep(10_000)
        clickPowerButton.exec()
    }
}