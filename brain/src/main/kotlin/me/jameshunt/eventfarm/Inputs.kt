package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import me.jameshunt.eventfarm.AtlasScientificEzoHum.HumidityInput
import me.jameshunt.eventfarm.AtlasScientificEzoHum.TemperatureInput
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

// lambda might be a Provider<InputEventManager> in dagger
class VPDFunction(private val config: Config, private val inputEventManager: () -> InputEventManager) : Input {
    data class Config(val id: UUID, val temperatureId: UUID, val humidityId: UUID)

    override val id: UUID = config.id

    // takes the event stream, grabs measurements it needs and spits out a new measurement calculated from other input
    // normal input might just be on a timer grabbing sensor data
    override fun getInputEvents(): Observable<Input.InputEvent> {
        val t = inputEventManager().getEventStream().filter { it.inputId == config.temperatureId }
        val h = inputEventManager().getEventStream().filter { it.inputId == config.humidityId }

        return Observable.combineLatest(t, h) { temp, humidity ->
            Input.InputEvent(id, Instant.now(), calcVPD(temp.value, humidity.value))
        }
    }

    private fun calcVPD(temp: TypedValue, humidity: TypedValue): TypedValue = TODO()
}

fun createAtlasScientficiEzoHum(): Device {
    val temperatureId = "00000000-0000-0000-0000-000000000005".let { UUID.fromString(it) }
    val humidityId = "00000000-0000-0000-0000-000000000006".let { UUID.fromString(it) }
    val tempInput = TemperatureInput(TemperatureInput.Config("temp", temperatureId))
    val humidityInput = HumidityInput(HumidityInput.Config("temp", humidityId))
    return AtlasScientificEzoHum(tempInput, humidityInput)
}

class AtlasScientificEzoHum(temperatureInput: TemperatureInput, humidityInput: HumidityInput) : Device {
    override val inputs: List<Input> = listOf(temperatureInput, humidityInput)
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
            if (disposable.hasInitialized()) return

            disposable = onSchedule.subscribe(
                {
                    if (it.isStarting) {
                        inputEventStream.onNext(Input.InputEvent(id, Instant.now(), getSensorValue()))
                    }
                },
                { throw it }
            )
        }

        override fun scheduleNext(previousCompleted: Scheduler.ScheduleItem?): Scheduler.ScheduleItem {
            val fiveSecondsLater = previousCompleted?.startTime?.plus(5, ChronoUnit.SECONDS)
            val startTime = fiveSecondsLater ?: Instant.now()
            return Scheduler.ScheduleItem(
                id,
                TypedValue.None,
                startTime,
                null
            )
        }
    }

    class HumidityInput(config: Config) : Input, Scheduler.SelfSchedulable {
        data class Config(val name: String, val id: UUID)

        override val id: UUID = config.id
        private val inputEventStream = PublishSubject.create<Input.InputEvent>()

        override fun getInputEvents(): Observable<Input.InputEvent> = inputEventStream

        private fun getSensorValue(): TypedValue {
            return TypedValue.Percent(0.5f)
        }

        private var disposable: Disposable? = null
        override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>) {
            if (disposable.hasInitialized()) return

            disposable = onSchedule.subscribe(
                {
                    if (it.isStarting) {
                        inputEventStream.onNext(Input.InputEvent(id, Instant.now(), getSensorValue()))
                    }
                },
                { throw it }
            )
        }

        override fun scheduleNext(previousCompleted: Scheduler.ScheduleItem?): Scheduler.ScheduleItem {
            val fifteenSecondsLater = previousCompleted?.startTime?.plus(15, ChronoUnit.SECONDS)
            val startTime = fifteenSecondsLater ?: Instant.now()
            return Scheduler.ScheduleItem(
                id,
                TypedValue.None,
                startTime,
                null
            )
        }
    }
}
