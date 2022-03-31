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

    data class Percent(val value: Float) : TypedValue() {
        init {
            check(value in 0f..1f)
        }
    }

    sealed class Pressure {
        data class Pascal(val value: Float) : Pressure()
        data class PSI(val value: Float) : Pressure()
        data class Bar(val value: Float) : Pressure()
    }

    @Deprecated("Pressure")
    data class Pascal(val value: Float) : TypedValue()
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
    data class InputEvent(val inputId: UUID, val time: Instant, val value: TypedValue)

    fun getInputEvents(): Observable<InputEvent>
}

interface Output : Scheduler.Schedulable, Configurable

// TODO: I think the device abstraction is useless. some inputs wouldn't even have a device like VPD
// TODO: serialize everything as a flat list with deviceId being a nullable field (used when looked at it grouped in ui, or deleting)
// TODO: but i don't really need a device at the code level
interface Device {
    val inputs: List<Input>
    val outputs: List<Output>
}

class Logger(private val config: Configurable.Config) {
    private val maxStringLengthOfLevel = "DEBUG".length
    fun debug(message: String) {
        log("DEBUG", message)
    }

    fun trace(message: String) {
        log("TRACE", message)
    }

    private fun log(level: String, message: String) {
        val levelRightAligned = level.prependIndent(" ".repeat(maxStringLengthOfLevel - level.length))
        val logMessage = "${Instant.now()}, ${levelRightAligned}, ${config.id}, ${config.className}, $message"
        println(logMessage)
    }
}
