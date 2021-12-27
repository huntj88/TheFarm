package me.jameshunt.thefarm

import me.jameshunt.thefarm.PowerManager.PlugAlias
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * alternate implementation would be to check hourly for what the light intensity/spectrum should be and updating it
 */

/**
 * alternate alternate implementation would be to leverage the scheduling feature on the smart plug and use
 * this scheduler to monitor that the plug is in the correct state, and fix it if its not.
 * Maybe use a lux meter to send a push notification if power is on, but the light is off
 */

class LightScheduler(
    private val timer: ScheduledThreadPoolExecutor,
    private val powerManager: PowerManager,
    private val logger: FarmLogger
) {
    private val tag = "LIGHT"
    private val turnOnTime = LocalTime.of(7, 0)
    private val turnOffTime = turnOnTime.plusHours(12)

    private val turnOnLights = Runnable {
        try {
            powerManager.setState(PlugAlias.Lights, on = true)
            logger.info(tag, "turned lights on")
        } catch (e: Exception) {
            logger.error(tag, "lighting on error", e)
            retry()
        }
    }
    private val turnOffLights = Runnable {
        try {
            powerManager.setState(PlugAlias.Lights, on = false)
            logger.info(tag, "turned lights off")
        } catch (e: Exception) {
            logger.error(tag, "lighting off error", e)
            retry()
        }
    }

    private fun retry() {
        val now = LocalTime.now()
        if (now > turnOnTime && now < turnOffTime) {
            timer.schedule(turnOnLights, 1, TimeUnit.MINUTES)
        } else {
            timer.schedule(turnOffLights, 1, TimeUnit.MINUTES)
        }
    }

    fun schedule() {
        scheduleTurnOnLightsTask(timer)
        scheduleTurnOffLightsTask(timer)
    }

    private fun scheduleTurnOnLightsTask(timer: ScheduledThreadPoolExecutor) {
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

        val delay = Instant.now().until(scheduledTime, ChronoUnit.SECONDS)
        timer.scheduleAtFixedRate(turnOnLights, delay, ChronoUnit.DAYS.duration.toSeconds(), TimeUnit.SECONDS)
    }

    private fun scheduleTurnOffLightsTask(timer: ScheduledThreadPoolExecutor) {
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

        val delay = Instant.now().until(scheduledTime, ChronoUnit.SECONDS)
        timer.scheduleAtFixedRate(turnOffLights, delay, ChronoUnit.DAYS.duration.toSeconds(), TimeUnit.SECONDS)
    }
}

private fun ZoneId.offset(): ZoneOffset {
    val offsetMilli = TimeZone.getTimeZone(id).rawOffset.toLong()
    return ZoneOffset.ofTotalSeconds(TimeUnit.MILLISECONDS.toSeconds(offsetMilli).toInt())
}
