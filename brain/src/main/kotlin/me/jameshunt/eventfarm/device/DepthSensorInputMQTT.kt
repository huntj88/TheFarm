package me.jameshunt.eventfarm.device

import io.reactivex.rxjava3.core.Observable
import me.jameshunt.eventfarm.core.*
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.time.Instant
import java.util.*

class DepthSensorInputMQTT(
    override val config: Config,
    private val logger: Logger,
    private val mqttManager: MQTTManager
) : Input {
    data class Config(
        override val id: UUID,
        override val className: String,
        val depthOfTankCentimeters: Float,
        val depthWhenFullCentimeters: Float,
        val mqttTopic: String,
    ) : Configurable.Config

    override fun getInputEvents(): Observable<Input.InputEvent> {
        return mqttManager
            .listen(config.mqttTopic)
            .flatMap { parseMessage(it) }
    }

    private fun parseMessage(mqttMessage: MqttMessage): Observable<Input.InputEvent> {
        val distanceCm = mqttMessage.payload.decodeToString().toFloat()

        val time = Instant.now()
        val depthCorrected = distanceCm - config.depthWhenFullCentimeters
        val depthOfTankCorrected = config.depthOfTankCentimeters - config.depthWhenFullCentimeters
        val missing = depthCorrected / depthOfTankCorrected
        val percentRemaining = (1f - missing).coerceIn(0f, 1f)
        return Observable.just(
            Input.InputEvent(
                inputId = config.id,
                index = null,
                time = time,
                value = TypedValue.Length.Centimeter(distanceCm)
            ),
            Input.InputEvent(
                inputId = config.id,
                index = null,
                time = time,
                value = TypedValue.Percent(percentRemaining)
            )
        )
    }
}