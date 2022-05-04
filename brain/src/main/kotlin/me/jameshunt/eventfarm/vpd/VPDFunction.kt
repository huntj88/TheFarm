package me.jameshunt.eventfarm.vpd

import io.reactivex.rxjava3.core.Observable
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.IInputEventManager
import me.jameshunt.eventfarm.core.Input
import me.jameshunt.eventfarm.core.TypedValue
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
