package me.jameshunt.eventfarm

import io.reactivex.rxjava3.disposables.Disposable

fun main() {
    DI()
}

class DI {
    private val configurable = mutableListOf<Configurable>()

    private val inputEventManager: InputEventManager = InputEventManager()
    private val scheduler: Scheduler = Scheduler(getSchedulable = { findId ->
        configurable.schedulable().first { it.id == findId }
    })

    private val configurableFactory = ConfigurableFactory(
        injectableComponents = mapOf(
            IInputEventManager::class.java.name to inputEventManager,
            Scheduler::class.java.name to scheduler,
        )
    )

    init {
        listOfJson.forEach { configurable.add(configurableFactory.deserialize(it)) }
        configurable.mapNotNull { it as? Input }.forEach { inputEventManager.addInput(it) }
        configurable.schedulable().mapNotNull { it as? Scheduler.SelfSchedulable }
            .forEach { scheduler.addSelfSchedulable(it) }
    }

    private fun List<Configurable>.schedulable(): List<Scheduler.Schedulable> {
        return this.mapNotNull { it as? Scheduler.Schedulable }
    }
}

fun Disposable?.hasInitialized(): Boolean = !(this == null || this.isDisposed)