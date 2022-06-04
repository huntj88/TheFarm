package me.jameshunt.eventfarm.vpd

import io.reactivex.rxjava3.core.Observable
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.IInputEventManager
import me.jameshunt.eventfarm.core.Input
import me.jameshunt.eventfarm.core.TypedValue
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow

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

        return Observable
            .combineLatest(tempStream, humidityStream) { t, h -> t to h }
            .filter { (t, h) ->
                val maxAge = Instant.now().minusSeconds(90L)
                t.time.isAfter(maxAge) && h.time.isAfter(maxAge)
            }
            .map { (t, h) ->
                val temperature = (t.value as TypedValue.Temperature).asCelsius()
                val humidity = (h.value as TypedValue.Percent)
                val vpd = calcVPD(temperature, humidity)
                Input.InputEvent(config.id, null, maxOf(t.time, h.time), vpd)
            }
            .throttleLast(100, TimeUnit.MILLISECONDS)
    }

    private fun calcVPD(temp: TypedValue.Temperature, humidity: TypedValue.Percent): TypedValue.Pressure {
        val tempC = temp.asCelsius().value
        val saturatedVaporPressure = 610.7f * (10f.pow(7.5f * tempC / (237.3f + tempC)))
        val vpdPascal = ((1f - humidity.value) / 1f) * saturatedVaporPressure
        return TypedValue.Pressure.Pascal(vpdPascal)
    }
}
