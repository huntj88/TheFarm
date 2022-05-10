package me.jameshunt.eventfarm.core

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence


class MQTTManager {
    data class TopicPayload(val topic: String, val payload: MqttMessage)

    private val incomingMessages = PublishSubject.create<TopicPayload>()

    private val client = MqttClient("tcp://localhost:1883", "brain", MemoryPersistence()).apply {
        setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable) {
                cause.printStackTrace()
                // After the connection is lost, it usually reconnects here
                println("disconnect，you can reconnect")
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                // The messages obtained after subscribe will be executed here
                println("Received message topic:$topic")
                println("Received message Qos:" + message.qos)
                println("Received message content:" + String(message.payload))
                incomingMessages.onNext(TopicPayload(topic, message))
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {
                println("deliveryComplete---------" + token.isComplete)
            }
        })
        val connOpts = MqttConnectOptions()
        connOpts.isCleanSession = true
        println("Connecting to broker")
        connect(connOpts)
        println("Connected")
    }

    fun listen(topic: String): Observable<MqttMessage> {
        client.subscribe(topic)
        return incomingMessages
            .filter { it.topic == topic }
            .map { it.payload }
            .doOnDispose { client.unsubscribe(topic) }
    }

    fun sendCommand(topic: String, data: String): Completable {
        TODO()
    }
}
