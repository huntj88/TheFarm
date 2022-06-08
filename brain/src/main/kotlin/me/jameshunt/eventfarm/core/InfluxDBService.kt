package me.jameshunt.eventfarm.core

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.Closeable
import java.util.*
import java.util.concurrent.TimeUnit

class InfluxDBService(
    private val logger: Logger,
    private val inputEventManager: IInputEventManager,
    private val getConfigurable: (UUID) -> Configurable
) : Closeable {
    private val token = Secrets.influxDBApiKey
    private val cloudUrl = "https://us-east-1-1.aws.cloud2.influxdata.com"
    private val bucket = "thefarm"
    private val org = "huntj88@gmail.com"
    private val client = InfluxDBClientFactory.create(cloudUrl, token.toCharArray())

    init {
        writeEventStreamToInfluxDB().subscribe()
    }

    private fun writeEventStreamToInfluxDB(): Completable {
        // TODO: local copy of data to sync up on reconnect? currently drops values when no internet
        // TODO: batch send data
        return inputEventManager.getEventStream().flatMapCompletable { inputEvent ->
            inputEvent.writeToInfluxDB().onErrorResumeNext {
                logger.error("Error writing $inputEvent to InfluxDB", it)
                Completable.complete()
                    .delay(15, TimeUnit.SECONDS) // retry in 15 seconds
                    .andThen { writeEventStreamToInfluxDB() }
            }
        }
    }

    private fun Input.InputEvent.writeToInfluxDB(): Completable {
        val className = getConfigurable(inputId)::class.java.simpleName

        val point: Point = Point
            .measurement("input")
            .addField(this)
            .addTag("inputId", inputId.toString())
            .addTag("inputIndex", index?.toString()) // could be null or could add cardinality
            .addTag("className", className) // will not add cardinality, dependent on inputId
            .time(time, WritePrecision.NS)

        return Completable
            .fromAction { client.writeApiBlocking.writePoint(bucket, org, point) }
            .subscribeOn(Schedulers.io())
    }

    override fun close() {
        client.close()
    }

    private fun Point.addField(inputEvent: Input.InputEvent): Point {
        val fieldName = inputEvent.value::class.simpleName!!
        return when (val value = inputEvent.value) {
            is TypedValue.Bool -> this.addField(fieldName, value.value)
            is TypedValue.Error -> this.addField(fieldName, value.err.stackTraceToString())
            is TypedValue.Length.Centimeter -> this.addField(fieldName, value.value)
            is TypedValue.Percent -> this.addField(fieldName, value.value)
            is TypedValue.Pressure.Bar -> this.addField(fieldName, value.value)
            is TypedValue.Pressure.PSI -> this.addField(fieldName, value.value)
            is TypedValue.Pressure.Pascal -> this.addField(fieldName, value.value)
            is TypedValue.Temperature.Celsius -> this.addField(fieldName, value.value)
            is TypedValue.Temperature.Kelvin -> this.addField(fieldName, value.value)
            is TypedValue.Watt -> this.addField(fieldName, value.value)
            is TypedValue.WattHour -> this.addField(fieldName, value.value)
            TypedValue.None -> throw IllegalArgumentException()
        }
    }
}