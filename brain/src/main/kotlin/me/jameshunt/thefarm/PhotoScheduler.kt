package me.jameshunt.thefarm

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class PhotoScheduler(private val timer: ScheduledThreadPoolExecutor, private val logger: FarmLogger) {
    private val tag = "PHOTO"
    private val androidPhoneIp = "192.168.1.73" // TODO: configurable

    private val takePhotoTask = Runnable {
        val runId = 1 // TODO: configurable
        try {
            takeAndroidPhoto(runId)
        } catch (e: Exception) {
            logger.error(tag, "error taking a picture", e)
        }
    }

    fun schedule() {
        timer.scheduleWithFixedDelay(takePhotoTask, 0, 1, TimeUnit.HOURS)
    }

    private fun takeAndroidPhoto(runId: Int) {
        val adbConnect = "adb connect $androidPhoneIp"

        val clickPowerButton = "adb shell input keyevent 26"
        val swipeUp = "adb shell input touchscreen swipe 930 880 930 380"
        val enterCode = "adb shell input text $phoneCode"
        val clickEnter = "adb shell input keyevent 66"
        val startApp = "adb shell am start -S -n me.jameshunt.remotecamera/.MainActivity --ei runId $runId"


        adbConnect.exec().also {
            logger.info(tag, it)
        }
        clickPowerButton.exec()
        swipeUp.exec()
        enterCode.exec()
        clickEnter.exec()
        startApp.exec().also {
            logger.info(tag, "taking a picture")
        }
        Thread.sleep(10_000)
        clickPowerButton.exec()
    }
}