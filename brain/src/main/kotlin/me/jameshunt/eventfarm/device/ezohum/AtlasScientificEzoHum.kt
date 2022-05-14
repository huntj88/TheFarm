package me.jameshunt.eventfarm.device.ezohum

import io.reactivex.rxjava3.core.Observable
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.Input
import me.jameshunt.eventfarm.core.MQTTManager
import me.jameshunt.eventfarm.core.TypedValue
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.time.Instant
import java.util.*

class AtlasScientificEzoHum(override val config: Config, private val mqttManager: MQTTManager) : Input { // TODO: make schedulable?
    data class Config(
        override val id: UUID,
        override val className: String = Config::class.java.name,
        val name: String,
        val mqttTopic: String,
    ) : Configurable.Config

    override fun getInputEvents(): Observable<Input.InputEvent> {
        return mqttManager
            .listen(config.mqttTopic)
            .flatMap { parseMessage(it) }
    }

    private fun parseMessage(mqttMessage: MqttMessage): Observable<Input.InputEvent> {
        val time = Instant.now()
        val (humidityPercent0To100, tempCelsius) = try {
            mqttMessage.payload.decodeToString()
                .also { check("," in it) { "Could not parse ezoHum values" } }
                .split(",")
                .let { it[0].toFloat() to it[1].toFloat() }
        } catch (e: Exception) {
            return Observable.just(Input.InputEvent(config.id, null, time, TypedValue.Error(e)))
        }

        return Observable.just(
            Input.InputEvent(
                inputId = config.id,
                index = null,
                time = time,
                value = TypedValue.Percent(humidityPercent0To100 / 100f)
            ),
            Input.InputEvent(
                inputId = config.id,
                index = null,
                time = time,
                value = TypedValue.Temperature.Celsius(tempCelsius)
            )
        )
    }
}