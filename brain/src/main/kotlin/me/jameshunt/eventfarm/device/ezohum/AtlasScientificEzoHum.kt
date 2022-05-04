package me.jameshunt.eventfarm.device.ezohum

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.Input
import me.jameshunt.eventfarm.core.Scheduler
import me.jameshunt.eventfarm.core.TypedValue
import java.time.Instant
import java.util.*

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