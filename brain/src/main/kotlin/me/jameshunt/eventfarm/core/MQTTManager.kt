package me.jameshunt.eventfarm.core

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.Closeable
import java.util.concurrent.TimeUnit

// TODO: security for mqtt, but starting with plaintext
class MQTTManager(private val logger: Logger) : Closeable {
    data class TopicPayload(val topic: String, val payload: MqttMessage)

    private val incomingMessages = PublishSubject.create<TopicPayload>()

    // mqtt broker and brain client use in memory persistence, so have to track subscribed topics here manually
    // in case of a disconnect
    private val subscribedTopics = mutableSetOf<String>()

    private val client by lazy {
        // will not be initialized unless there are mqtt inputs
        startMQTTBroker()
        createClient()
    }

    // TODO: set up persistence on broker and brain client. Delete reconnect resubscribe logic below
    //  (set isCleanSession=false, autoReconnect=true after)
    private val reconnectDisposable = Observable.interval(30, TimeUnit.SECONDS)
        .filter { this::client.isLazyInitialized }
        .map { client.isConnected }
        .doOnNext { isConnected ->
            if (!isConnected) {
                try {
                    logger.debug("mqtt client reconnecting to broker")
                    client.reconnect()
                } catch (e: Exception) {
                    logger.error("could not reconnect to mqtt broker", e)
                }
            }
        }
        .delay(5, TimeUnit.SECONDS)
        .map { wasConnected -> wasConnected to client.isConnected }
        .doOnNext { (wasConnected, isConnected) ->
            if (!wasConnected && isConnected) {
                logger.debug("mqtt client reconnected to broker")
                subscribedTopics.forEach { client.subscribe(it) }
            }

            if (!wasConnected && !isConnected) {
                startMQTTBroker()
            }
        }
        .subscribe()

    fun listen(topic: String): Observable<MqttMessage> {
        subscribedTopics.add(topic)
        client.subscribe(topic)
        return incomingMessages
            .filter { it.topic == topic }
            .map { it.payload }
            .doOnDispose {
                subscribedTopics.remove(topic)
                client.unsubscribe(topic)
            }
            .observeOn(Schedulers.io())
    }

    fun sendCommand(topic: String, data: String): Completable {
        TODO()
    }

    override fun close() {
        logger.debug("stopping mqtt broker")
        reconnectDisposable.dispose()
        stopMQTTBroker()
        incomingMessages.onComplete()
    }

    private fun createClient(): MqttClient {
        // TODO: parametrize ip, env variable?
        return MqttClient("tcp://192.168.1.83:1883", "brain", MemoryPersistence()).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable) {
                    logger.warn("mqtt client disconnected from broker", cause)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    incomingMessages.onNext(TopicPayload(topic, message))
                }

                override fun deliveryComplete(token: IMqttDeliveryToken) {
                    logger.debug("""deliveryComplete---------${token.isComplete}""")
                }
            })
            val connOpts = MqttConnectOptions()
            connOpts.isCleanSession = true

            try {
                logger.debug("mqtt client connecting to broker")
                connect(connOpts)
            } catch (e: Exception) {
                logger.error("could not connect to mqtt broker", e)
            }
        }
    }

    private fun startMQTTBroker() {
        val downloadImageCmd = "docker pull eclipse-mosquitto:2.0.14"
        val startMQTTBrokerCmd =
            "docker run -itd -p 1883:1883 eclipse-mosquitto:2.0.14 mosquitto -c /mosquitto-no-auth.conf"

        try {
            logger.debug("downloading MQTT message broker docker image")
            downloadImageCmd.exec(timeoutSeconds = 40)
        } catch (e: Exception) {
            // TODO: handle docker not being installed? docker install script?
            logger.error("could not download docker image, is docker installed?", e)
        }

        try {
            logger.debug("starting MQTT message broker docker image")
            startMQTTBrokerCmd.exec()
        } catch (e: Exception) {
            // TODO: more detailed error messages
            logger.warn("could not start or MQTT broker already started", e)
        }
    }

    private fun stopMQTTBroker() {
        // TODO: String.exec() extension not working here, Fix?
        val process = ProcessBuilder(
            "/bin/bash", "-c",
            "docker ps -q --filter ancestor=eclipse-mosquitto:2.0.14 | xargs docker stop"
        ).start().also { it.waitFor(20, TimeUnit.SECONDS) }

        try {
            process.exitValue()
        } catch (e: IllegalThreadStateException) {
            throw Exception("command timed out", e)
        }

        if (process.exitValue() != 0) {
            throw Exception(process.errorStream.bufferedReader().readText())
        }
        println(process.inputStream.bufferedReader().readText().trim())
    }
}
