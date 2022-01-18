package me.jameshunt.thefarm

import java.io.File
import java.net.NetworkInterface
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

// TODO: adb does not work by default on raspberry pi (armv7)
class PhotoScheduler(private val timer: ScheduledThreadPoolExecutor, private val logger: FarmLogger) {
    private val tag = "PHOTO"
    private val androidPhoneIp = "192.168.1.73" // TODO: configurable

    private val platformToolsDirectory: File = File(libDirectory,"platform-tools")

    init {
        when(platformToolsDirectory.exists()) {
            true -> logger.info(tag, "adb already installed")
            false -> {
                logger.info(tag, "installing adb")
                "wget https://dl.google.com/android/repository/platform-tools-latest-linux.zip".exec(libDirectory)
                "unzip -q platform-tools-latest-linux".exec(libDirectory)
                "rm platform-tools-latest-linux.zip".exec(libDirectory)
            }
        }
    }

    private val takePhotoTask = Runnable {
        val runId = 1 // TODO: configurable
        try {
            takeAndroidPhoto(runId)
        } catch (e: Exception) {
            logger.error(tag, "error taking a picture", e)
        }
    }

    @Deprecated("deprecated until adb raspberry pi workaround found")
    fun schedule() {
        getLocalNetworkIp()
        timer.scheduleWithFixedDelay(takePhotoTask, 0, 1, TimeUnit.HOURS)
    }

    private fun takeAndroidPhoto(runId: Int) {
        val adbConnect = "adb connect $androidPhoneIp"

        val clickPowerButton = "adb shell input keyevent 26"
        val swipeUp = "adb shell input touchscreen swipe 930 880 930 380"
        val enterCode = "adb shell input text $phoneCode"
        val clickEnter = "adb shell input keyevent 66"
        val startApp = "adb shell am start -S -n me.jameshunt.remotecamera/.MainActivity --ei runId $runId --es destinationIp ${getLocalNetworkIp()}"


        adbConnect.exec(platformToolsDirectory).also {
            logger.info(tag, it)
        }
        clickPowerButton.exec(platformToolsDirectory)
        swipeUp.exec(platformToolsDirectory)
        enterCode.exec(platformToolsDirectory)
        clickEnter.exec(platformToolsDirectory)
        startApp.exec(platformToolsDirectory).also {
            logger.info(tag, "taking a picture")
        }
        Thread.sleep(10_000)
        clickPowerButton.exec(platformToolsDirectory)
    }

    private fun getLocalNetworkIp(): String {
        return NetworkInterface.getNetworkInterfaces().asSequence().toList()
            .flatMap { it.inetAddresses.asSequence().toList() }
            .first { it.hostAddress.startsWith("192.168") }
            .hostAddress
    }
}