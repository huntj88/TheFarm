package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

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
            Input.InputEvent(config.id, null, Instant.now(), calcVPD(temp.value, humidity.value))
        }.throttleLatest(100, TimeUnit.MILLISECONDS)
    }

    private fun calcVPD(temp: TypedValue, humidity: TypedValue): TypedValue {
        val tempValue = (temp as? TypedValue.Temperature)?.asCelsius()?.value
            ?: throw IllegalArgumentException("expected temperature")
        val humidityValue =
            (humidity as? TypedValue.Percent)?.value ?: throw IllegalArgumentException("expected percent")

        // TODO: correct math
        return TypedValue.Pressure.Pascal(650f)
    }
}

// TODO: consolidate like HS300.Inputs?
class AtlasScientificEzoHum {
    class TemperatureInput(override val config: Config) : Input, Scheduler.Schedulable {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String
        ) : Configurable.Config

        private val inputEventStream = PublishSubject.create<Input.InputEvent>()

        override fun getInputEvents(): Observable<Input.InputEvent> = inputEventStream

        private fun getSensorValue(): TypedValue {
            return TypedValue.Temperature.Celsius(20f)
        }

        override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
            return onSchedule.subscribe(
                {
                    if (it.isStarting) {
                        inputEventStream.onNext(Input.InputEvent(config.id, null, Instant.now(), getSensorValue()))
                    }
                },
                { throw it }
            )
        }
    }

    class HumidityInput(override val config: Config) : Input, Scheduler.Schedulable {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String
        ) : Configurable.Config

        private val inputEventStream = PublishSubject.create<Input.InputEvent>()

        override fun getInputEvents(): Observable<Input.InputEvent> = inputEventStream

        private fun getSensorValue(): TypedValue {
            return TypedValue.Percent(0.5f)
        }

        override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
            return onSchedule.subscribe(
                {
                    if (it.isStarting) {
                        inputEventStream.onNext(Input.InputEvent(config.id, null, Instant.now(), getSensorValue()))
                    }
                },
                { throw it }
            )
        }
    }
}
