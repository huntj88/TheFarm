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
        val tempStream = inputEventManager.getEventStream()
            .filter { it.inputId == config.temperatureId }
            .filter { it.value is TypedValue.Temperature }

        val humidityStream = inputEventManager.getEventStream()
            .filter { it.inputId == config.humidityId }
            .filter { it.value is TypedValue.Percent }

        return Observable.combineLatest(tempStream, humidityStream) { temp, humidity ->
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

class AtlasScientificEzoHum(override val config: Config) : Input, Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String = Config::class.java.name,
        val name: String
    ) : Configurable.Config

    private val inputEventStream = PublishSubject.create<Input.InputEvent>()

    override fun getInputEvents(): Observable<Input.InputEvent> = inputEventStream

    override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
        return onSchedule.subscribe(
            {
                if (it.isStarting) {
                    val time = Instant.now()
                    val (temperature, humidity) = getSensorData()
                    inputEventStream.onNext(Input.InputEvent(config.id, null, time, temperature))
                    inputEventStream.onNext(Input.InputEvent(config.id, null, time, humidity))
                }
            },
            { throw it }
        )
    }

    private data class EzoHumData(
        val temperature: TypedValue.Temperature,
        val humidity: TypedValue.Percent
    )

    private fun getSensorData(): EzoHumData {
        // TODO: get actual sensor data
        return EzoHumData(temperature = TypedValue.Temperature.Celsius(20f), humidity = TypedValue.Percent(0.5f))
    }
}
