package me.jameshunt.eventfarm.core

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import me.jameshunt.thefarm.exec
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

// TODO: security for mqtt, but starting with plaintext
class MQTTManager(private val logger: Logger) {
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

    init {
        // TODO: set up persistence on broker and brain client. Delete reconnect resubscribe logic below
        //  (set isCleanSession=false, autoReconnect=true after)
        Observable.interval(30, TimeUnit.SECONDS)
            .filter { this::client.isLazyInitialized }
            .map { client.isConnected }
            .doOnNext { isConnected ->
                if (!isConnected) {
                    logger.debug("mqtt client reconnecting to broker")
                    client.reconnect()
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
    }

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
            logger.debug("mqtt client connecting to broker")
            connect(connOpts)
            logger.debug("mqtt client connected to broker")
        }
    }

    private fun startMQTTBroker() {
        val downloadImageCmd = "docker pull eclipse-mosquitto:2.0.14"
        val startMQTTBrokerCmd =
            "docker run -itd -p 1883:1883 eclipse-mosquitto:2.0.14 mosquitto -c /mosquitto-no-auth.conf"

        try {
            logger.debug("downloading MQTT message broker docker image")
            downloadImageCmd.exec()
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
}


/**
 * Returns true if a lazy property reference has been initialized, or if the property is not lazy.
 */
// TODO: util file?
val KProperty0<*>.isLazyInitialized: Boolean
    get() {
        if (this !is Lazy<*>) return true

        // Prevent IllegalAccessException from JVM access check on private properties.
        val originalAccessLevel = isAccessible
        isAccessible = true
        val isLazyInitialized = (getDelegate() as Lazy<*>).isInitialized()
        // Reset access level.
        isAccessible = originalAccessLevel
        return isLazyInitialized
    }
