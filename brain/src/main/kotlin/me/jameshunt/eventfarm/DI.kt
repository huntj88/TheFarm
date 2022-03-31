package me.jameshunt.eventfarm

import me.jameshunt.eventfarm.Scheduler.Schedulable
import me.jameshunt.eventfarm.Scheduler.ScheduleItem
import java.time.Instant

fun main() {
    DI
}

object DI {
    val configurable = mutableListOf<Configurable>()

    val loggerFactory = LoggerFactory()
    val inputEventManager: InputEventManager = InputEventManager()
    val scheduler: Scheduler = Scheduler(loggerFactory) { findId ->
        configurable.first { it.config.id == findId } as? Schedulable ?: throw IllegalStateException()
    }

    private val configurableFactory = ConfigurableFactory(inputEventManager, scheduler, loggerFactory)

    init {
        listOfJson
            .map { configurableFactory.configurableFromJson(it) }
            .let { configurable.addAll(it) }

        configurable
            .mapNotNull { it as? Input }
            .forEach { inputEventManager.addInput(it) }

        val vpdControllerSchedule = ScheduleItem(
            id = configurable.first { it is VPDController }.config.id,
            data = TypedValue.None,
            startTime = Instant.now(),
            endTime = null // Instant.now().plusSeconds(20) // can have the controller run for a limited amount of time
        )
        scheduler.schedule(vpdControllerSchedule)

//        val ecPhControllerId = configurable.schedulable().first { it is ECPHExclusiveLockController }.config.id
//        val ecPhControllerSchedule = Scheduler.ScheduleItem(ecPhControllerId, TypedValue.None, Instant.now(), endTime)
//        scheduler.schedule(ecPhControllerSchedule)

        val ezoHumControllerSchedule = ScheduleItem(
            id = configurable.first { it is AtlasScientificEzoHumController }.config.id,
            data = TypedValue.None,
            startTime = Instant.now(),
            endTime = null
        )
        scheduler.schedule(ezoHumControllerSchedule)
    }
}


// TODO: some sort of safe delete for configurable stuff. make sure there are no dependencies on it,
//  maybe create an DAG graph
