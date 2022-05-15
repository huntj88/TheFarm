package me.jameshunt.eventfarm.customcontroller

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.core.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

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
        val waterDepthIndex: Int?,  // TODO: waterDepthInputIndex
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
        return inputEventManager
            .waitForValueOrDefaultThenInterval(
                periodMillis = config.periodMillis,
                inputId = config.waterDepthInputId,
                inputIndex = config.waterDepthIndex,
                default = TypedValue.Percent(1f) // TODO: use latest value from db before restart
            )
            .map { percent -> getPumpDuration(percent) }
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
        if (reservoirPercentEvent.time.isBefore(Instant.now().minus(6, ChronoUnit.HOURS))) {
            logger.warn("Reservoir depth value too old for pump check, using default behavior", null)
            // disregard depth and use normal duration
            return config.durationMillis
        }

        val reservoirPercent = (reservoirPercentEvent.value as TypedValue.Percent).value
        return if (reservoirPercent < 0.02f) {
            0L
        } else {
            config.durationMillis
        }
    }
}
