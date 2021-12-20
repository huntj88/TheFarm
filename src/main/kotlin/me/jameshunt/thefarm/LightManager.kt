package me.jameshunt.thefarm

import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit


class LightManager {
    private val turnOnTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 0))
    private val turnOffTime = turnOnTime.plusHours(12)

    @Volatile
    private var on = false

    private val turnOnLights = object : TimerTask() {
        override fun run() {
            on = true
            "Lights: ON".log()
        }
    }

    private val turnOffLights = object : TimerTask() {
        override fun run() {
            on = false
            "Lights: OFF".log()
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

        "Lights ON scheduled for: ${scheduledTime.atOffset(defaultOffset)}".log()

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

        "Lights OFF scheduled for: ${scheduledTime.atOffset(defaultOffset)}".log()

        timer.scheduleAtFixedRate(turnOffLights, Date.from(scheduledTime), ChronoUnit.DAYS.duration.toMillis())
    }
}

private fun ZoneId.offset(): ZoneOffset {
    val offsetMilli = TimeZone.getTimeZone(id).rawOffset.toLong()
    return ZoneOffset.ofTotalSeconds(TimeUnit.MILLISECONDS.toSeconds(offsetMilli).toInt())
}
