package me.jameshunt.eventfarm.vpd

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.core.*
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

// super basic vpd controller
class VPDController(
    override val config: Config,
    private val scheduler: Scheduler,
    private val inputEventManager: IInputEventManager,
    private val logger: Logger
) : Configurable, Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String = Config::class.java.name,
        val vpdInputId: UUID,
        val humidifierOutputId: UUID,
        val humidifierOutputIndex: Int?
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

    private fun handle(): Observable<Input.InputEvent> {
        return inputEventManager
            .getEventStream()
            .filter { it.inputId == config.vpdInputId && it.index == config.humidifierOutputIndex }
            .throttleLatest(10, TimeUnit.SECONDS)
            .filter {
                val vpd = it.value as TypedValue.Pressure.Pascal
                vpd.value > 925
            }
            .doOnNext {
                logger.debug("VPD too high, raising humidity")
                val startTime = Instant.now()
                val endTime = startTime.plusSeconds(7)
                scheduler.schedule(
                    Scheduler.ScheduleItem(
                        config.humidifierOutputId,
                        null,
                        TypedValue.Bool(true),
                        startTime,
                        endTime
                    )
                )
            }
    }
}