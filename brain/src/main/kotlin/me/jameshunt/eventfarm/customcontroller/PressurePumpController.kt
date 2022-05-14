package me.jameshunt.eventfarm.customcontroller

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.core.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

// TODO: controller that verifies tank gets lower when pump is on
//  (check water level, pump until timer done, check water level and compare to before pumping)
// TODO: controller for allowing air to escape from the line (pump would stop working, probably due to H2O2 releasing air bubbles)
class PressurePumpController(
    override val config: Config,
    private val inputEventManager: IInputEventManager,
    private val scheduler: Scheduler,
    private val logger: Logger,
) : Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val pressurePumpOnOffOutputId: UUID,
        val pressurePumpOnOffOutputIndex: Int?,
        val waterDepthInputId: UUID,
        val waterDepthIndex: Int?,
        val periodMillis: Long,
        val durationMillis: Long,
    ) : Configurable.Config

    override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
        return onSchedule.switchMap {
            when {
                it.isStarting -> handle()
                it.isEnding -> Observable.empty()
                else -> Observable.error(IllegalStateException("Should not be possible"))
            }
        }.subscribe(
            {},
            { throw it }
        )
    }


    private fun handle(): Observable<Long> {
        val percentRemainingReservoir = inputEventManager.getEventStream()
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
            .withLatestFrom(percentRemainingReservoir) { _, percent -> getPumpDuration(percent) }
            .doOnNext { durationMillis ->
                val now = Instant.now()
                val end = now.plusMillis(durationMillis)
                scheduler.schedule(
                    Scheduler.ScheduleItem(
                        config.pressurePumpOnOffOutputId,
                        config.pressurePumpOnOffOutputIndex,
                        TypedValue.Bool(true),
                        now,
                        end
                    )
                )
            }
    }

    private fun getPumpDuration(reservoirPercentEvent: Input.InputEvent): Long {
        // only use reservoir percent if there is a recent value
        return if (reservoirPercentEvent.time.isAfter(Instant.now().minus(6, ChronoUnit.HOURS))) {
            val reservoirPercent = (reservoirPercentEvent.value as TypedValue.Percent).value
            if (reservoirPercent < 0.02f) {
                0L
            } else {
                config.durationMillis
            }
        } else {
            logger.warn("Reservoir depth value too old for pump check, using default behavior", null)
            // disregard depth and use normal
            config.durationMillis
        }
    }
}
