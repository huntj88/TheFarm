package me.jameshunt.thefarm

import me.jameshunt.thefarm.PowerManager.PlugAlias
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * alternate implementation would be to check hourly for what the light intensity/spectrum should be and updating it
 */

class LightScheduler(private val powerManager: PowerManager) {
    private val turnOnTime = LocalTime.of(7, 0)
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
        val now = LocalTime.now()
        val defaultOffset = ZoneOffset.systemDefault().offset()

        val scheduledTime: Instant = if (now > turnOnTime && now < turnOffTime) {
            turnOnLights.run()
            LocalDateTime.of(LocalDate.now().plusDays(1), turnOnTime).toInstant(defaultOffset)
        } else if (now > turnOffTime) {
            LocalDateTime.of(LocalDate.now().plusDays(1), turnOnTime).toInstant(defaultOffset)
        } else {
            LocalDateTime.of(LocalDate.now(), turnOnTime).toInstant(defaultOffset)
        }

        timer.scheduleAtFixedRate(turnOnLights, Date.from(scheduledTime), ChronoUnit.DAYS.duration.toMillis())
    }

    private fun scheduleTurnOffLightsTask(timer: Timer) {
        val now = LocalTime.now()
        val defaultOffset = ZoneOffset.systemDefault().offset()

        val scheduledTime: Instant = if (now < turnOnTime) {
            turnOffLights.run()
            LocalDateTime.of(LocalDate.now(), turnOffTime).toInstant(defaultOffset)
        } else if (now > turnOffTime) {
            turnOffLights.run()
            LocalDateTime.of(LocalDate.now().plusDays(1), turnOffTime).toInstant(defaultOffset)
        } else {
            LocalDateTime.of(LocalDate.now(), turnOffTime).toInstant(defaultOffset)
        }

        timer.scheduleAtFixedRate(turnOffLights, Date.from(scheduledTime), ChronoUnit.DAYS.duration.toMillis())
    }
}

private fun ZoneId.offset(): ZoneOffset {
    val offsetMilli = TimeZone.getTimeZone(id).rawOffset.toLong()
    return ZoneOffset.ofTotalSeconds(TimeUnit.MILLISECONDS.toSeconds(offsetMilli).toInt())
}
