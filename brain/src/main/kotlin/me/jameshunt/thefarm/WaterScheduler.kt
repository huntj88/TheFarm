package me.jameshunt.thefarm

import me.jameshunt.thefarm.PowerManager.*
import java.util.*
import java.util.concurrent.Executors

class WaterScheduler(private val powerManager: PowerManager) {
    private val executor = Executors.newSingleThreadExecutor()

    private val turnOnWaterTask = object : TimerTask() {
        override fun run() {
            powerManager.setState(PlugAlias.Water, on = true)
            executor.execute {
                Thread.sleep(5 * 1000L)
                powerManager.setState(PlugAlias.Water, on = false)
            }
        }
    }

    fun schedule(timer: Timer) {
        timer.schedule(turnOnWaterTask, 0, 1 * 60 * 1000L)
    }
}