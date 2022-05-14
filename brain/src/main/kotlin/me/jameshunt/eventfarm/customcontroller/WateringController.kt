package me.jameshunt.eventfarm.customcontroller

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.core.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

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
        val schedulableId: UUID,
        val schedulableIndex: Int?,
        val waterDepthInputId: UUID,
        val waterDepthIndex: Int?,
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
        val percentRemainingReservoir = eventManager.getEventStream()
            .filter { it.inputId == config.waterDepthInputId && it.index == config.waterDepthIndex }
            .filter { it.value is TypedValue.Percent }
            .startWith(
                // TODO: use latest value from db before restart

                // stream needs at least one value to start
                // otherwise will not start if rebooted and tank sensor doesn't work
                // fake input event set to yesterday. will be disregarded as too old
                Observable.just(
                    Input.InputEvent(
                        config.waterDepthInputId,
                        config.waterDepthIndex,
                        Instant.now().minus(1, ChronoUnit.DAYS),
                        TypedValue.Percent(1f)
                    )
                )
            )


        // collect values for a minute after startup, and then start the interval
        // (throwing away fake input or old db input in favor of recent data
        return percentRemainingReservoir
            .take(1, TimeUnit.MINUTES)
            .takeLast(1).map { Unit } // ensures interval only started once
            .switchIfEmpty { Observable.just(Unit) } // ensures the interval is started
            .flatMap { Observable.interval(0, config.periodMillis, TimeUnit.MILLISECONDS) }
            .withLatestFrom(percentRemainingReservoir) { _, percent -> getWateringDurationMillis(percent) }
            .doOnNext { durationMillis ->
                val now = Instant.now()
                val end = now.plusMillis(durationMillis)
                scheduler.schedule(
                    Scheduler.ScheduleItem(
                        config.schedulableId,
                        config.schedulableIndex,
                        TypedValue.Bool(true),
                        now,
                        end
                    )
                )
            }
    }

    private fun getWateringDurationMillis(reservoirPercentEvent: Input.InputEvent): Long {
        // only use reservoir percent if there is a recent value
        return if (reservoirPercentEvent.time.isAfter(Instant.now().minus(6, ChronoUnit.HOURS))) {
            val reservoirPercent = (reservoirPercentEvent.value as TypedValue.Percent).value
            val conserveWaterReservoirPercent = config.conserveWaterReservoirPercent.also { TypedValue.Percent(it) }
            val conserveWaterUsagePercent =
                config.conserveWaterUsagePercent.also { TypedValue.Percent(it) } // validate percent too
            if (reservoirPercent < conserveWaterReservoirPercent) {
                // if: conserveWaterReservoirPercent = 0.15
                // if: conserveWaterUsagePercent = 0.6
                // then: when less than 15% remaining in tank only use 60% watering duration
                (config.durationMillis * conserveWaterUsagePercent).toLong()
            } else {
                config.durationMillis
            }
        } else {
            logger.warn("Reservoir depth value too old for conserve water check, using default behavior", null)
            // disregard depth and use normal
            config.durationMillis
        }
    }
}