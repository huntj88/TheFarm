package me.jameshunt.eventfarm.controller

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.*
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

class MyLightingController(
    override val config: Config,
    private val inputEventManager: IInputEventManager,
    private val scheduler: Scheduler
) : Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val lightOnOffInputId: UUID,
        val inputIndex: Int?,
        val lightOnOffOutputId: UUID,
        val outputIndex: Int?,
        val turnOnTime: LocalTime,
        val turnOffTime: LocalTime
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

    private fun handle(): Observable<Boolean> {
        return inputEventManager.getEventStream()
            .filter { it.isOnOffStateForPlug() }
            .map { (it.value as TypedValue.Bool).value }
            .doOnNext { isOn ->
                val shouldBeOn = LocalTime.now() >= config.turnOnTime && LocalTime.now() < config.turnOffTime
                if (isOn != shouldBeOn) {
                    scheduler.schedule(
                        Scheduler.ScheduleItem(
                            config.lightOnOffOutputId,
                            config.outputIndex,
                            TypedValue.Bool(shouldBeOn),
                            Instant.now(),
                            null
                        )
                    )
                }
            }
    }

    private fun Input.InputEvent.isOnOffStateForPlug(): Boolean {
        return inputId == config.lightOnOffInputId
            && index == config.inputIndex
            && value is TypedValue.Bool
    }
}