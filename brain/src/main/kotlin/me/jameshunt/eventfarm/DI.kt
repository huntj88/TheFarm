package me.jameshunt.eventfarm

import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.*
import java.util.concurrent.TimeUnit

fun getVPDInput(getInputEventManager: () -> InputEventManager): Input {
    val vpdInputId = "00000000-0000-0000-0000-000000000007".let { UUID.fromString(it) }
    val tempInputId = "00000000-0000-0000-0000-000000000005".let { UUID.fromString(it) }
    val humidityInputId = "00000000-0000-0000-0000-000000000006".let { UUID.fromString(it) }
    return VPDFunction(
        VPDFunction.Config(vpdInputId, tempInputId, humidityInputId),
        inputEventManager = { getInputEventManager() })
}

class DI {
    val devices = listOf(createPowerStrip(), createAtlasScientficiEzoHum())
    val inputs: List<Input> = devices.flatMap { it.inputs } + listOf(getVPDInput { inputEventManager })
    val outputs: List<Output> = devices.flatMap { it.outputs }
    val inputEventManager: InputEventManager = InputEventManager(inputs)
    val compositeDisposable = CompositeDisposable()

    // in dagger bind schedulable
    val schedulable =
        inputs.mapNotNull { it as? Scheduler.Schedulable } + outputs.mapNotNull { it as? Scheduler.SelfSchedulable }

    val scheduler: Scheduler = Scheduler().apply {
        schedulable.mapNotNull { it as? Scheduler.SelfSchedulable }.forEach { addSelfSchedulable(it) }
        compositeDisposable.add(this.loop())
    }

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