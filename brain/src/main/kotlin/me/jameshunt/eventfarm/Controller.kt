package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.time.Instant
import java.time.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * two [PeriodicController] could mimic the [AtlasScientificEzoHumController], but not the [ECPHExclusiveLockController]
 **/

// TODO: HS300 could come with a prebuilt controller since there are so many configurables, and maybe you can
//   disable it, and use a different controllers if different behavior was required

class PeriodicController(
    override val config: Config,
    private val scheduler: Scheduler
) : Configurable, Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val schedulableId: UUID,
        val periodMillis: Long,
        val durationMillis: Long?
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
        return Observable.interval(0, config.periodMillis, TimeUnit.MILLISECONDS).doOnNext { _ ->
            val now = Instant.now()
            val end = config.durationMillis?.let { now.plusMillis(it) }
            scheduler.schedule(Scheduler.ScheduleItem(config.schedulableId, TypedValue.None, now, end))
        }
    }
}

class AtlasScientificEzoHumController(
    override val config: Config,
    private val scheduler: Scheduler
) : Configurable, Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val humidityInputId: UUID,
        val temperatureInputId: UUID
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
        return Observable.interval(0, 15, TimeUnit.SECONDS).doOnNext { _ ->

            val now = Instant.now()
            val ecScheduleItem = Scheduler.ScheduleItem(config.humidityInputId, TypedValue.None, now, null)
            val phScheduleItem = Scheduler.ScheduleItem(config.temperatureInputId, TypedValue.None, now, null)

            scheduler.schedule(ecScheduleItem)
            scheduler.schedule(phScheduleItem)
        }
    }
}


// TODO: using humidity and temp ids for ph and ec in json
class ECPHExclusiveLockController(
    override val config: Config,
    private val scheduler: Scheduler
) : Configurable, Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val ecInputId: UUID,
        val phInputId: UUID
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
        return Observable.interval(0, 60, TimeUnit.SECONDS).doOnNext { _ ->
            val now = Instant.now()
            val switchTime = now.plusSeconds(30)
            val endTime = switchTime.plusSeconds(30)

            val ecScheduleItem = Scheduler.ScheduleItem(config.ecInputId, TypedValue.None, now, switchTime)
            val phScheduleItem = Scheduler.ScheduleItem(config.phInputId, TypedValue.None, switchTime, endTime)

            scheduler.schedule(ecScheduleItem)
            scheduler.schedule(phScheduleItem)
        }
    }
}

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
        val humidifierOutputId: UUID
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
            .filter { it.inputId == config.vpdInputId }
            .throttleLatest(10, TimeUnit.SECONDS)
            .filter {
                val vpd = it.value as TypedValue.Pascal
                vpd.value > 925
            }
            .doOnNext {
                logger.debug("VPD too high, raising humidity")
                val startTime = Instant.now()
                val endTime = startTime.plusSeconds(7)
                scheduler.schedule(
                    Scheduler.ScheduleItem(
                        config.humidifierOutputId,
                        TypedValue.Bool(true),
                        startTime,
                        endTime
                    )
                )
            }
    }
}

class HS300InputController(override val config: Config, private val scheduler: Scheduler) : Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,

        val index0OnOffInputId: UUID,
        val index1OnOffInputId: UUID,
        val index2OnOffInputId: UUID,
        val index3OnOffInputId: UUID,
        val index4OnOffInputId: UUID,
        val index5OnOffInputId: UUID,

        val totalWattInputId: UUID,
        val totalWattHourInputId: UUID,

        val index0WattInputId: UUID,
        val index1WattInputId: UUID,
        val index2WattInputId: UUID,
        val index3WattInputId: UUID,
        val index4WattInputId: UUID,
        val index5WattInputId: UUID,

        val index0WattHourInputId: UUID,
        val index1WattHourInputId: UUID,
        val index2WattHourInputId: UUID,
        val index3WattHourInputId: UUID,
        val index4WattHourInputId: UUID,
        val index5WattHourInputId: UUID
    ) : Configurable.Config

    private val inputIds = listOf(
        config.index0OnOffInputId,
        config.index1OnOffInputId,
        config.index2OnOffInputId,
        config.index3OnOffInputId,
        config.index4OnOffInputId,
        config.index5OnOffInputId,
        config.totalWattInputId,
        config.totalWattHourInputId,
        config.index0WattInputId,
        config.index1WattInputId,
        config.index2WattInputId,
        config.index3WattInputId,
        config.index4WattInputId,
        config.index5WattInputId,
        config.index0WattHourInputId,
        config.index1WattHourInputId,
        config.index2WattHourInputId,
        config.index3WattHourInputId,
        config.index4WattHourInputId,
        config.index5WattHourInputId
    )

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
        val period = 100L
        return Observable.interval(0, period, TimeUnit.SECONDS).doOnNext { _ ->
            val now = Instant.now()

            // space requests out evenly over the period
            val timeBetweenRequests = period / inputIds.size

            inputIds
                .mapIndexed { i, inputId ->
                    Scheduler.ScheduleItem(
                        inputId,
                        TypedValue.None,
                        now.plusSeconds(i * timeBetweenRequests),
                        null
                    )
                }
                .forEach { scheduler.schedule(it) }
        }
    }
}

class MyLightingController(
    override val config: Config,
    private val inputEventManager: IInputEventManager,
    private val scheduler: Scheduler
) : Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val lightOnOffInputId: UUID,
        val lightOnOffOutputId: UUID,
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
            .filter { it.inputId == config.lightOnOffInputId }
            .map { (it.value as TypedValue.Bool).value }
            .doOnNext { isOn ->
                val shouldBeOn = LocalTime.now() >= config.turnOnTime || LocalTime.now() < config.turnOffTime
                if (isOn != shouldBeOn) {
                    scheduler.schedule(
                        Scheduler.ScheduleItem(
                            config.lightOnOffOutputId,
                            TypedValue.Bool(shouldBeOn),
                            Instant.now(),
                            null
                        )
                    )
                }
            }
    }
}
