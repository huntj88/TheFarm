package me.jameshunt.thefarm

import me.jameshunt.thefarm.PowerManager.*
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * alternate implementation would be to check hourly for what the light intensity/spectrum should be and updating it
 */

class LightScheduler(private val powerManager: PowerManager) {
    private val turnOnTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 0))
    private val turnOffTime = turnOnTime.plusHours(12)

    private val turnOnLights = object : TimerTask() {
        override fun run() {
            powerManager.setState(PlugAlias.Lights, on = true)
        }
    }

    private val turnOffLights = object : TimerTask() {
        override fun run() {
            powerManager.setState(PlugAlias.Lights, on = false)
        }
    }

    fun schedule(timer: Timer) {
        scheduleTurnOnLightsTask(timer)
        scheduleTurnOffLightsTask(timer)
    }

    private fun scheduleTurnOnLightsTask(timer: Timer) {
        val now = LocalDateTime.now()
        val defaultOffset = ZoneOffset.systemDefault().offset()

        val scheduledTime: Instant = if (now > turnOnTime && now < turnOffTime) {
            turnOnLights.run()
            turnOnTime.plusDays(1).toInstant(defaultOffset)
        } else if (now > turnOffTime) {
            turnOnTime.plusDays(1).toInstant(defaultOffset)
        } else {
            turnOnTime.toInstant(defaultOffset)
        }

        timer.scheduleAtFixedRate(turnOnLights, Date.from(scheduledTime), ChronoUnit.DAYS.duration.toMillis())
    }

    private fun scheduleTurnOffLightsTask(timer: Timer) {
        val now = LocalDateTime.now()
        val defaultOffset = ZoneOffset.systemDefault().offset()

        val scheduledTime: Instant = if (now > turnOffTime) {
            turnOffTime.plusDays(1).toInstant(defaultOffset)
        } else {
            turnOffTime.toInstant(defaultOffset)
        }

        timer.scheduleAtFixedRate(turnOffLights, Date.from(scheduledTime), ChronoUnit.DAYS.duration.toMillis())
    }
}

private fun ZoneId.offset(): ZoneOffset {
    val offsetMilli = TimeZone.getTimeZone(id).rawOffset.toLong()
    return ZoneOffset.ofTotalSeconds(TimeUnit.MILLISECONDS.toSeconds(offsetMilli).toInt())
}
