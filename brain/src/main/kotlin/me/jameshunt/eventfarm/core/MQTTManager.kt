package me.jameshunt.eventfarm.core

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible


class MQTTManager(private val logger: Logger) {
    data class TopicPayload(val topic: String, val payload: MqttMessage)

    private val incomingMessages = PublishSubject.create<TopicPayload>()

    // mqtt broker and brain client use in memory persistence, so have to track subscribed topics here manually
    // in case of a disconnect
    private val subscribedTopics = mutableSetOf<String>()

    private val client by lazy {
        // will not be initialized unless there are mqtt inputs
        // TODO: start mqtt broker
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
    }

    fun sendCommand(topic: String, data: String): Completable {
        TODO()
    }

    private fun createClient(): MqttClient {
        return MqttClient("tcp://localhost:1883", "brain", MemoryPersistence()).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable) {
                    logger.error("mqtt client disconnected from broker", cause)
                }

                @Throws(Exception::class)
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
