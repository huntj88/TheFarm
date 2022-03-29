package me.jameshunt.eventfarm

import io.reactivex.rxjava3.disposables.Disposable
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

fun getVPDController(scheduler: Scheduler, inputEventManager: InputEventManager): VPDController {
    val id = "00000000-0000-0000-0001-000000000000".let { UUID.fromString(it) }
    val vpdInputId = "00000000-0000-0000-0000-000000000007".let { UUID.fromString(it) }
    val humidifierOutputId = "00000000-0000-0000-0000-000000000152".let { UUID.fromString(it) }
//    return VPDPIDController(scheduler, inputEventManager, vpdInputId, humidifierOutputId)
    val config = VPDController.Config(id, vpdInputId = vpdInputId, humidifierOutputId = humidifierOutputId)
    return VPDController(config, scheduler, inputEventManager)
}

// super basic vpd controller
class VPDController(
    override val config: Config,
    private val scheduler: Scheduler,
    private val inputEventManager: IInputEventManager
) : Controller {
    data class Config(
        override val id: UUID,
        override val className: String = Config::class.java.name,
        val vpdInputId: UUID,
        val humidifierOutputId: UUID
    ) : Configurable.Config

    fun handle(): Disposable {
        return inputEventManager
            .getEventStream()
            .filter { it.inputId == config.vpdInputId }
            .throttleLatest(10, TimeUnit.SECONDS)
            .filter {
                val vpd = it.value as TypedValue.Pascal
                vpd.value > 925
            }
            .subscribe({
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
            }, { throw it })
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