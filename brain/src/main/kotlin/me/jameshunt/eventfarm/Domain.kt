package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import java.time.Instant
import java.util.*

sealed class TypedValue {
    object None : TypedValue()
    data class Celsius(val value: Float) : TypedValue()
    data class Percent(val value: Float) : TypedValue() {
        init {
            check(value in 0f..1f)
        }
    }

    data class Pascal(val value: Float) : TypedValue()
    data class WattHour(val value: Float) : TypedValue()
    data class Watt(val value: Float) : TypedValue()
    data class Bool(val value: Boolean) : TypedValue()
    data class VaporPressureDeficit(val value: Float) : TypedValue()
}

interface Input {
    data class InputEvent(val inputId: UUID, val time: Instant, val value: TypedValue)

    val id: UUID
    fun getInputEvents(): Observable<InputEvent>
}

interface Output {
    val id: UUID
}

// functions get access to DI tree?
//interface Function {
//
//}

interface Device {
    val inputs: List<Input>
    val outputs: List<Output>
}