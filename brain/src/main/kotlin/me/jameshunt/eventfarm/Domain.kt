package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import java.time.Instant
import java.util.*

sealed class TypedValue {
    object None : TypedValue()
    sealed class Temperature : TypedValue() {
        data class Celsius(val value: Float) : Temperature()
        data class Kelvin(val value: Float) : Temperature()

        fun asCelsius(): Celsius = when (this) {
            is Celsius -> this
            is Kelvin -> TODO()
        }

        fun asKelvin(): Kelvin = when (this) {
            is Celsius -> TODO()
            is Kelvin -> this
        }
    }

    sealed class Length: TypedValue() {
        data class Centimeter(val value: Float): Length()
    }

    data class Percent(val value: Float) : TypedValue() {
        init {
            check(value in 0f..1f)
        }
    }

    sealed class Pressure: TypedValue() {
        data class Pascal(val value: Float) : Pressure()
        data class PSI(val value: Float) : Pressure()
        data class Bar(val value: Float) : Pressure()
    }

    data class WattHour(val value: Float) : TypedValue()
    data class Watt(val value: Float) : TypedValue()
    data class Bool(val value: Boolean) : TypedValue()
}

interface Configurable {
    // data that will be serialized to preserve settings
    interface Config {
        val id: UUID
        val className: String
    }

    val config: Config
}

interface Input : Configurable {
    // if input has multiple input values of the same type an index is used to differentiate
    data class InputEvent(val inputId: UUID, val index: Int?, val time: Instant, val value: TypedValue)

    fun getInputEvents(): Observable<InputEvent>
}

interface Output : Scheduler.Schedulable, Configurable

// TODO: I think the device abstraction is useless. some inputs wouldn't even have a device like VPD
// TODO: serialize everything as a flat list with deviceId being a nullable field (used when looked at it grouped in ui, or deleting)
// TODO: but i don't really need a device at the code level
//interface Device {
//    val inputs: List<Input>
//    val outputs: List<Output>
//}

interface Logger {
    fun trace(message: String)
    fun debug(message: String)
    fun error(message: String, throwable: Throwable?)
}

class LoggerFactory {
    // TODO could add an loggingEnabled flag for each config somehow. if false return a noOp implementation
    fun create(config: Configurable.Config): Logger = DefaultConfigLogger(config)
}

// TODO: real logging lib?
class DefaultConfigLogger(private val config: Configurable.Config): Logger {
    private val maxStringLengthOfLevel = "DEBUG".length
    override fun debug(message: String) {
        log("DEBUG", message)
    }

    override fun trace(message: String) {
        log("TRACE", message)
    }

    override fun error(message: String, throwable: Throwable?) {
        log("ERROR", "$message, error: ${throwable?.stackTraceToString()}")
    }

    private fun log(level: String, message: String) {
        val levelRightAligned = level.prependIndent(" ".repeat(maxStringLengthOfLevel - level.length))
        val logMessage = "${Instant.now()}, ${levelRightAligned}, ${config.id}, ${config.className}, $message"
        println(logMessage)
    }
}

class DefaultLogger(private val name: String): Logger {
    private val maxStringLengthOfLevel = "DEBUG".length
    override fun debug(message: String) {
        log("DEBUG", message)
    }

    override fun trace(message: String) {
        log("TRACE", message)
    }

    override fun error(message: String, throwable: Throwable?) {
        log("ERROR", "$message, error: ${throwable?.stackTraceToString()}")
    }

    private fun log(level: String, message: String) {
        val levelRightAligned = level.prependIndent(" ".repeat(maxStringLengthOfLevel - level.length))
        val logMessage = "${Instant.now()}, ${levelRightAligned}, $name, $message"
        println(logMessage)
    }
}
