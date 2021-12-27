package me.jameshunt.thefarm

import me.jameshunt.thefarm.PowerManager.PlugAlias
import java.util.*

class WaterScheduler(private val powerManager: PowerManager) {
    private val turnOnWaterTask = object : TimerTask() {
        override fun run() {
            powerManager.setState(PlugAlias.Water, on = true)
            Thread.sleep(5 * 1000L)
            powerManager.setState(PlugAlias.Water, on = false)
        }
    }

    fun schedule(timer: Timer) {
        timer.schedule(turnOnWaterTask, 0, 20 * 1000L)
    }
}