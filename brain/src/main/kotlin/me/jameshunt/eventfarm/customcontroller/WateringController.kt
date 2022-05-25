package me.jameshunt.eventfarm.customcontroller

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.core.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

// TODO: handle restarts gracefully, check when last watered using values in a DB (input on event, or schedule time?)
/**
 * Controls how often plants are watered.
 * When the amount of water remaining in the reservoir gets low, conserve water mode is enabled.
 */
class WateringController(
    override val config: Config,
    private val scheduler: Scheduler,
    private val eventManager: IInputEventManager,
    private val logger: Logger,
) : Configurable, Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val wateringOutputId: UUID,
        val wateringOutputIndex: Int?,
        val waterDepthInputId: UUID,
        val waterDepthIndex: Int?, // TODO: waterDepthInputIndex
        val periodMillis: Long,
        val durationMillis: Long,
        val conserveWaterUsagePercent: Float, // 0f-1f
        val conserveWaterReservoirPercent: Float, // 0f-1f
    ) : Configurable.Config

    override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
        return onSchedule.switchMap {
            when {
                it.isStarting -> handle()
                it.isEnding -> Observable.empty()
                else -> Observable.error(IllegalStateException("Should not be possible"))
            }
        }.subscribe({}, { throw it })
    }

    private fun handle(): Observable<Long> {
        return eventManager
            .waitForValueOrDefaultThenInterval(
                periodMillis = config.periodMillis,
                inputId = config.waterDepthInputId,
                inputIndex = config.waterDepthIndex,
                default = TypedValue.Percent(1f) // TODO: use latest value from db before restart
            )
            .map { percent -> getWateringDurationMillis(percent) }
            .doOnNext { durationMillis ->
                val now = Instant.now()
                val end = now.plusMillis(durationMillis)
                scheduler.schedule(
                    Scheduler.ScheduleItem(
                        config.wateringOutputId,
                        config.wateringOutputIndex,
                        TypedValue.Bool(true),
                        now,
                        end
                    )
                )
            }
    }

    private fun getWateringDurationMillis(reservoirPercentEvent: Input.InputEvent): Long {
        // only use reservoir percent if there is a recent value
        if (reservoirPercentEvent.time.isBefore(Instant.now().minus(6, ChronoUnit.HOURS))) {
            logger.warn("Reservoir depth value too old for conserve water check, using default behavior", null)
            // disregard depth and use normal duration
            return config.durationMillis
        }

        val reservoirPercent = (reservoirPercentEvent.value as TypedValue.Percent).value

        val conserveWaterReservoirPercent = config.conserveWaterReservoirPercent.withPercentValidation()
        val conserveWaterUsagePercent = config.conserveWaterUsagePercent.withPercentValidation()
        return if (reservoirPercent < conserveWaterReservoirPercent) {
            val formattedPercent = "${reservoirPercent * 100f}".takeLast(5)
            logger.warn("Reservoir depth at $formattedPercent, Conserving water", null)
            // if: conserveWaterReservoirPercent = 0.15
            // if: conserveWaterUsagePercent = 0.6
            // then: when less than 15% remaining in tank only use 60% watering duration
            (config.durationMillis * conserveWaterUsagePercent).toLong()
        } else {
            config.durationMillis
        }
    }

    private fun Float.withPercentValidation(): Float {
        return this.also { TypedValue.Percent(it) }
    }
}