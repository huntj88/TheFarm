package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit

interface IInputEventManager {
    fun getEventStream(): Observable<Input.InputEvent>
}

class InputEventManager(private val inputs: List<Input>) : IInputEventManager {
    // inputs could be an Observable<List<Input>> if the inputs needed to be changed over time
    // instead of being initialized with everything needed
    override fun getEventStream(): Observable<Input.InputEvent> {
        val retryInputs = inputs.map {
            it.getInputEvents().retryWhen {
                Observable.timer(30, TimeUnit.SECONDS)
            }
        }
        return (1 until retryInputs.size)
            .map { retryInputs[it] }
            .fold(retryInputs.first()) { acc, input -> acc.mergeWith(input) }
    }
}