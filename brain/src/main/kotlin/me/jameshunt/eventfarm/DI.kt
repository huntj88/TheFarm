package me.jameshunt.eventfarm

import io.reactivex.rxjava3.disposables.Disposable
import java.time.Instant

fun main() {
    DI()
}

class DI {
    val configurable = mutableListOf<Configurable>()

    val inputEventManager: InputEventManager = InputEventManager()
    val scheduler: Scheduler = Scheduler(getSchedulable = { findId ->
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
        configurable.mapNotNull { it as? Scheduler.SelfSchedulable }.forEach { scheduler.addSelfSchedulable(it) }

        val vpdControllerId = configurable.schedulable().first { it is VPDController }.id
        val endTime = null // Instant.now().plusSeconds(20) // can have the controller run for a limited amount of time
        val controllerSchedule = Scheduler.ScheduleItem(vpdControllerId, TypedValue.None, Instant.now(), endTime)
        scheduler.schedule(controllerSchedule)
    }

    private fun List<Configurable>.schedulable(): List<Scheduler.Schedulable> {
        return this.mapNotNull { it as? Scheduler.Schedulable }
    }
}

fun Disposable?.hasInitialized(): Boolean = !(this == null || this.isDisposed)


// TODO: some sort of safe delete for configurable stuff. make sure there are no dependencies on it,
//  maybe create an DAG graph
