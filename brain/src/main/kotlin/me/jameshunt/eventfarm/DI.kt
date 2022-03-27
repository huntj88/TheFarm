package me.jameshunt.eventfarm

import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

class DI {
    val devices = listOf(createPowerStrip(), createAtlasScientficiEzoHum())
    val inputs: List<Input> = devices.flatMap { it.inputs }
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