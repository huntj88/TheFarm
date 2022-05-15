package me.jameshunt.eventfarm.core

import io.reactivex.rxjava3.core.Observable
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

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

    sealed class Length : TypedValue() {
        data class Centimeter(val value: Float) : Length()
    }

    data class Percent(val value: Float) : TypedValue() {
        init {
            check(value in 0f..1f)
        }
    }

    sealed class Pressure : TypedValue() {
        data class Pascal(val value: Float) : Pressure()
        data class PSI(val value: Float) : Pressure()
        data class Bar(val value: Float) : Pressure()

        fun asPascal(): Pascal = when (this) {
            is Pascal -> this
            is Bar -> TODO()
            is PSI -> TODO()
        }
    }

    data class WattHour(val value: Float) : TypedValue()
    data class Watt(val value: Float) : TypedValue()
    data class Bool(val value: Boolean) : TypedValue()
    data class Error(val err: Throwable) : TypedValue()
}

/**
 *  Widest scope possible should be used for [T].
 *  [TypedValue.Pressure] is preferred over [TypedValue.Pressure.Pascal]
 *  [TypedValue.Temperature] is preferred over [TypedValue.Temperature.Celsius]
 *
 *  inputEventManager.waitForValueOrDefaultThenInterval<TypedValue.Temperature>(
 *      default = TypedValue.Temperature.Celsius(100)
 *      ...
 *  )
 **/
inline fun <reified T : TypedValue> IInputEventManager.waitForValueOrDefaultThenInterval(
    periodMillis: Long,
    inputId: UUID,
    inputIndex: Int?,
    default: T
): Observable<Input.InputEvent> {
    val inputValues = this.getEventStream()
        .filter { it.inputId == inputId && it.index == inputIndex && it.value is T }
        .startWith(
            // stream needs at least one value to start
            // otherwise will not start if rebooted and input sensor doesn't work
            // fake input event set to a year ago. will be disregarded as too old, or replaced by new data
            Observable.just(
                Input.InputEvent(inputId, inputIndex, Instant.now().minus(365, ChronoUnit.DAYS), default)
            )
        )

    // collect values for a minute after startup, and then start the interval
    // throwing away fake input or old db input in favor of recent data if there is any
    return inputValues
        .take(1, TimeUnit.MINUTES)
        .takeLast(1).map { Unit } // ensures interval only started once, and that the full minute is waited
        .switchIfEmpty { Observable.just(Unit) } // ensures the interval is started
        .flatMap { Observable.interval(0, periodMillis, TimeUnit.MILLISECONDS) }
        .withLatestFrom(inputValues) { _, inputEvent -> inputEvent }
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

    /**
     * return an InputEvent or multiple InputEvents with different TypedValues
     * will only ever have one subscriber [InputEventManager]
     * */
    fun getInputEvents(): Observable<InputEvent>
}

interface Output : Scheduler.Schedulable, Configurable

interface Logger {
    fun trace(message: String)
    fun debug(message: String)
    fun warn(message: String, throwable: Throwable?)
    fun error(message: String, throwable: Throwable?)
}

class LoggerFactory {
    // TODO reuse loggers via hashmap
    // TODO could add an loggingEnabled flag for each config somehow. if false return a noOp implementation
    fun create(config: Configurable.Config): Logger = DefaultConfigLogger(config)
}

// TODO: real logging lib?
class DefaultConfigLogger(private val config: Configurable.Config) : Logger {
    private val maxStringLengthOfLevel = "WARNING".length
    override fun trace(message: String) {
        log("TRACE", message)
    }

    override fun debug(message: String) {
        log("DEBUG", message)
    }

    override fun warn(message: String, throwable: Throwable?) {
        log("WARNING", "$message, error: ${throwable?.stackTraceToString()}")
    }

    override fun error(message: String, throwable: Throwable?) {
        log("ERROR", "$message, error: ${throwable?.stackTraceToString()}")
    }

    private fun log(level: String, message: String) {
        val color = when (level in listOf("WARNING", "ERROR")) {
            true -> "\u001b[31m" // warnings,errors are red
            false -> "\u001b[0m"
        }
        val levelRightAligned = level.prependIndent(" ".repeat(maxStringLengthOfLevel - level.length))
        val logMessage =
            "$color${Instant.now()}, ${levelRightAligned}, ${Thread.currentThread().name}, ${config.id}, ${config.className}, $message"
        println(logMessage)
    }
}

class DefaultLogger(private val name: String) : Logger {
    private val maxStringLengthOfLevel = "WARNING".length
    override fun trace(message: String) {
        log("TRACE", message)
    }

    override fun debug(message: String) {
        log("DEBUG", message)
    }

    override fun warn(message: String, throwable: Throwable?) {
        log("WARNING", "$message, error: ${throwable?.stackTraceToString()}")
    }

    override fun error(message: String, throwable: Throwable?) {
        log("ERROR", "$message, error: ${throwable?.stackTraceToString()}")
    }

    private fun log(level: String, message: String) {
        val color = when (level in listOf("WARNING", "ERROR")) {
            true -> "\u001b[31m" // warnings,errors are red
            false -> "\u001b[0m"
        }
        val levelRightAligned = level.prependIndent(" ".repeat(maxStringLengthOfLevel - level.length))
        val logMessage = "$color${Instant.now()}, ${levelRightAligned}, ${Thread.currentThread().name}, $name, $message"
        println(logMessage)
    }
}
