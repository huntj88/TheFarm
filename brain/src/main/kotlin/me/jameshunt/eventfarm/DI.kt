package me.jameshunt.eventfarm

import java.util.concurrent.TimeUnit

class DI {
    val devices = listOf(createPowerStrip())
    val inputs: List<Input> = devices.flatMap { it.inputs }
    val inputEventManager: InputEventManager = InputEventManager(inputs)

    // in dagger bind schedulable
    val schedulable = inputs.mapNotNull { it as? Scheduler.Schedulable } // + outputs

    val scheduler: Scheduler = Scheduler(schedulable)

    fun pidIntervalsUsingBuffer() {
        inputEventManager
            .getEventStream()
            .buffer(1, TimeUnit.SECONDS)
    }

    fun logEvents() {
        inputEventManager
            .getEventStream()
            .subscribe {
                TODO("sqlite io")
            }
    }
}