package me.jameshunt.eventfarm

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
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

fun getVPDController(scheduler: Scheduler, inputEventManager: InputEventManager): VPDController {
    val vpdInputId = "00000000-0000-0000-0000-000000000007".let { UUID.fromString(it) }
    val humidifierOutputId = "00000000-0000-0000-0000-000000000152".let { UUID.fromString(it) }
    return VPDController(scheduler, inputEventManager, vpdInputId, humidifierOutputId)
}

class DI {
    val compositeDisposable = CompositeDisposable()
    val devices = listOf(createPowerStrip(), createAtlasScientficiEzoHum())
    val inputs: List<Input> = devices.flatMap { it.inputs } + listOf(getVPDInput { inputEventManager })
    val outputs: List<Output> = devices.flatMap { it.outputs }
    val inputEventManager: InputEventManager = InputEventManager(inputs)

    // in dagger bind schedulable
    val schedulable =
        inputs.mapNotNull { it as? Scheduler.Schedulable } + outputs.mapNotNull { it as? Scheduler.Schedulable }

    val scheduler: Scheduler = Scheduler(getSchedulable = { findId -> schedulable.first { it.id == findId } }).apply {
        schedulable.mapNotNull { it as? Scheduler.SelfSchedulable }.forEach { addSelfSchedulable(it) }
        compositeDisposable.add(this.loop())
    }

    val vpdController = getVPDController(scheduler, inputEventManager).apply {
        compositeDisposable.add(handle())
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

fun Disposable?.hasInitialized(): Boolean = !(this == null || this.isDisposed)