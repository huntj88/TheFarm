package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import me.jameshunt.eventfarm.AtlasScientificEzoHum.HumidityInput
import me.jameshunt.eventfarm.AtlasScientificEzoHum.TemperatureInput
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

// lambda might be a Provider<InputEventManager> in dagger
class VPDFunction(override val config: Config, private val inputEventManager: IInputEventManager) : Input {
    data class Config(
        override val id: UUID,
        override val className: String = Config::class.java.name,
        val temperatureId: UUID,
        val humidityId: UUID
    ) : Configurable.Config

    // takes the event stream, grabs measurements it needs and spits out a new measurement calculated from other input
    // normal input might just be on a timer grabbing sensor data
    override fun getInputEvents(): Observable<Input.InputEvent> {
        val t = inputEventManager.getEventStream().filter { it.inputId == config.temperatureId }
        val h = inputEventManager.getEventStream().filter { it.inputId == config.humidityId }

        return Observable.combineLatest(t, h) { temp, humidity ->
            Input.InputEvent(config.id, Instant.now(), calcVPD(temp.value, humidity.value))
        }.debounce(100, TimeUnit.MILLISECONDS)
    }

    private fun calcVPD(temp: TypedValue, humidity: TypedValue): TypedValue {
        val tempValue = (temp as? TypedValue.Celsius)?.value ?: throw IllegalArgumentException("expected celsius")
        val humidityValue =
            (humidity as? TypedValue.Percent)?.value ?: throw IllegalArgumentException("expected percent")

        // TODO: correct math
        return TypedValue.Pascal(950f)
    }
}

fun createAtlasScientficiEzoHum(): Device {
    val temperatureId = "00000000-0000-0000-0000-000000000005".let { UUID.fromString(it) }
    val humidityId = "00000000-0000-0000-0000-000000000006".let { UUID.fromString(it) }
    val tempInput = TemperatureInput(TemperatureInput.Config(temperatureId, name = "temp"))
    val humidityInput = HumidityInput(HumidityInput.Config(humidityId, name = "humidity"))
    return AtlasScientificEzoHum(tempInput, humidityInput)
}

class AtlasScientificEzoHum(temperatureInput: TemperatureInput, humidityInput: HumidityInput) : Device {
    override val inputs: List<Input> = listOf(temperatureInput, humidityInput)
    override val outputs: List<Output> = emptyList()

    class TemperatureInput(override val config: Config) : Input, Scheduler.SelfSchedulable {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String
        ) : Configurable.Config

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
            val fiveSecondsAfterLast = previousCompleted?.startTime?.plus(5, ChronoUnit.SECONDS)
            val now = Instant.now()
            val startTime = if (fiveSecondsAfterLast?.isBefore(now) == true) {
                now
            } else {
                fiveSecondsAfterLast ?: now
            }
            return Scheduler.ScheduleItem(
                id,
                TypedValue.None,
                startTime,
                null
            )
        }
    }

    class HumidityInput(override val config: Config) : Input, Scheduler.SelfSchedulable {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String
        ) : Configurable.Config

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
            val fifteenSecondsAfterLast = previousCompleted?.startTime?.plus(15, ChronoUnit.SECONDS)
            val now = Instant.now()
            val startTime = if (fifteenSecondsAfterLast?.isBefore(now) == true) {
                now
            } else {
                fifteenSecondsAfterLast ?: now
            }

            return Scheduler.ScheduleItem(
                id,
                TypedValue.None,
                startTime,
                null
            )
        }
    }
}
