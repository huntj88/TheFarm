package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
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

class AtlasScientificEzoHum : Device {
    override val inputs: List<Input> = emptyList()
    override val outputs: List<Output> = emptyList()

    class TemperatureInput(config: Config) : Input, Scheduler.SelfSchedulable {
        data class Config(val name: String, val id: UUID, val ip: String)

        override val id: UUID = config.id
        private val inputEventStream = PublishSubject.create<Input.InputEvent>()

        override fun getInputEvents(): Observable<Input.InputEvent> = inputEventStream

        private fun getSensorValue(): TypedValue {
            // TODO
            return TypedValue.Celsius(20f)
        }

        override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>) {
            // TODO: handle disposable
            onSchedule.subscribe(
                {
                    if (it.isStarting) {
                        val value = getSensorValue()
                        inputEventStream.onNext(Input.InputEvent(id, Instant.now(), value))
                    }
                },
                { throw it }
            )
        }

        override fun schedule(previousCompleted: Scheduler.ScheduleItem?): Scheduler.ScheduleItem {
            val fiveMinutesLater = previousCompleted?.startTime?.plus(5, ChronoUnit.MINUTES)
            val startTime = fiveMinutesLater ?: Instant.now()
            return Scheduler.ScheduleItem(
                UUID.randomUUID(), // TODO
                TypedValue.None,
                startTime,
                null
            )
        }
    }
}
