package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

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


// TODO: using humidity and temp ids for ph and ec
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
    private val inputEventManager: IInputEventManager
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
                println("handling")
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

// very bad implementation of PID, unfinished
class VPDPIDController(
    private val scheduler: Scheduler,
    private val inputEventManager: InputEventManager,
    private val vpdInputId: UUID,
    private val humidifierOutputId: UUID
) {
    private var lastTime: Instant = Instant.now()
    private var errSum: Float = 0f
    private var lastErr: Float = 0f
    var kp: Float = 1f
    var ki: Float = 0.1f
    var kd: Float = 0f
    var setpoint: Float = 925f

    fun handle(): Disposable {
        return inputEventManager
            .getEventStream()
//            .doOnNext { println(it) }
            .filter { it.inputId == vpdInputId }
            .debounce(10, TimeUnit.SECONDS)
//            .debounce(500, TimeUnit.MILLISECONDS)
//            .filter { (it.value is TypedValue.Pascal) }
//            .map { (it.value as TypedValue.Pascal) }
            .subscribe({
//                val startTime = Instant.now()
//                val endTime = startTime.plusSeconds(5)
//                scheduler.schedule(
//                    Scheduler.ScheduleItem(
//                        humidifierOutputId,
//                        TypedValue.Bool(true),
//                        startTime,
//                        endTime
//                    )
//                )
                val pidOutput = compute(input = (it.value as TypedValue.Pascal).value)
                // convert this into a scheduleItem to correct an environmental variable
                val endTime = { TODO() }

                scheduler.schedule(
                    Scheduler.ScheduleItem(
                        humidifierOutputId,
                        TypedValue.Bool(true),
                        Instant.now(),
                        endTime()
                    )
                )
            }, {
                throw it
            })
    }

    private fun compute(input: Float): Float {
        /*How long since we last calculated*/
        val now = Instant.now()
        val timeChange = lastTime.until(now, ChronoUnit.SECONDS)

        /*Compute all the working error variables*/
        val error = setpoint - input
        errSum += (error * timeChange)
        val dErr = (error - lastErr) / timeChange

        /*Compute PID Output*/
        val output = kp * error + ki * errSum + kd * dErr

        /*Remember some variables for next time*/
        lastErr = error
        lastTime = now

        println(error)
        println(errSum)
        println(output)
        return output
    }
}