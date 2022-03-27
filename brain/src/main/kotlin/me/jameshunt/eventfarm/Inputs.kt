package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import me.jameshunt.eventfarm.AtlasScientificEzoHum.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

// lambda might be a Provider<InputEventManager> in dagger
class VPDFunction(override val id: UUID, private val inputEventManager: () -> InputEventManager) : Input {
    // takes the event stream, grabs measurements it needs and spits out a new measurement calculated from other input
    // normal input might just be on a timer grabbing sensor data
    override fun getInputEvents(): Observable<Input.InputEvent> {
        val temperatureId = UUID.randomUUID()
        val humidityId = UUID.randomUUID()
        val t = inputEventManager().getEventStream().filter { it.inputId == temperatureId }
        val h = inputEventManager().getEventStream().filter { it.inputId == humidityId }

        return Observable.combineLatest(t, h) { temp, humidity ->
            Input.InputEvent(id, Instant.now(), calcVPD(temp.value, humidity.value))
        }
    }

    private fun calcVPD(temp: TypedValue, humidity: TypedValue): TypedValue = TODO()
}

fun createAtlasScientficiEzoHum(): Device {
    val tempInput = TemperatureInput(TemperatureInput.Config("temp", UUID.randomUUID()))
    return AtlasScientificEzoHum(tempInput)
}

class AtlasScientificEzoHum(temperatureInput: TemperatureInput) : Device {
    override val inputs: List<Input> = listOf(temperatureInput)
    override val outputs: List<Output> = emptyList()

    class TemperatureInput(config: Config) : Input, Scheduler.SelfSchedulable {
        data class Config(val name: String, val id: UUID)

        override val id: UUID = config.id
        private val inputEventStream = PublishSubject.create<Input.InputEvent>()

        override fun getInputEvents(): Observable<Input.InputEvent> = inputEventStream

        private fun getSensorValue(): TypedValue {
            return TypedValue.Celsius(20f)
        }

        private var disposable: Disposable? = null
        override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>) {
            disposable?.dispose()// TODO: can i not subscribe to the new one and use existing subscription?
            disposable = onSchedule.subscribe(
                {
                    if (it.isStarting) {
                        val value = getSensorValue()
                        inputEventStream.onNext(Input.InputEvent(id, Instant.now(), value))
                    }
                },
                { throw it }
            )
        }

        override fun scheduleNext(previousCompleted: Scheduler.ScheduleItem?): Scheduler.ScheduleItem {
            val fiveMinutesLater = previousCompleted?.startTime?.plus(15, ChronoUnit.SECONDS)
            val startTime = fiveMinutesLater ?: Instant.now()
            return Scheduler.ScheduleItem(
                id,
                TypedValue.None,
                startTime,
                null
            )
        }
    }
}
