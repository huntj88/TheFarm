package me.jameshunt.eventfarm

import io.reactivex.rxjava3.disposables.Disposable

fun main() {
    DI()
}

class DI {
    val inputEventManager: InputEventManager = InputEventManager()

    val configurable = mutableListOf<Configurable>()

    // TODO make this not copy every time...
    val schedulable: List<Scheduler.Schedulable>
        get() = configurable.mapNotNull { it as? Scheduler.Schedulable }

    val scheduler: Scheduler = Scheduler(getSchedulable = { findId ->
        schedulable.first { it.id == findId }
    })

    val configurableFactory = ConfigurableFactory(
        injectableComponents = mapOf(
            IInputEventManager::class.java.name to inputEventManager,
            Scheduler::class.java.name to scheduler,
        )
    )

    init {
        listOfJson.forEach { configurable.add(configurableFactory.deserialize(it)) }
        configurable.mapNotNull { it as? Input }.forEach { inputEventManager.addInput(it) }
        schedulable.mapNotNull { it as? Scheduler.SelfSchedulable }.forEach { scheduler.addSelfSchedulable(it) }
    }
}

fun Disposable?.hasInitialized(): Boolean = !(this == null || this.isDisposed)