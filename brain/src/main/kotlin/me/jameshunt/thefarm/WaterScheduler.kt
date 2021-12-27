package me.jameshunt.thefarm

import me.jameshunt.thefarm.PowerManager.PlugAlias
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class WaterScheduler(
    private val timer: ScheduledThreadPoolExecutor,
    private val powerManager: PowerManager,
    private val logger: FarmLogger
) {
    private val tag = "WATER"

    private val turnOnWaterTask = Runnable {
        try {
            powerManager.setState(PlugAlias.Water, on = true)
            logger.info(tag, "started watering")
            Thread.sleep(5 * 1000L)
            powerManager.setState(PlugAlias.Water, on = false)
            logger.info(tag, "finished watering")
        } catch (e: Exception) {
            logger.error(tag, "watering error", e)
        }
    }

    fun schedule() {
        timer.scheduleWithFixedDelay(turnOnWaterTask, 0, 20, TimeUnit.SECONDS)
    }
}