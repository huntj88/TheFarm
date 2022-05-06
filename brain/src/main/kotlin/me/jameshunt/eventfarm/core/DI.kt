package me.jameshunt.eventfarm.core

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.jameshunt.eventfarm.core.Scheduler.Schedulable
import me.jameshunt.eventfarm.core.Scheduler.ScheduleItem
import me.jameshunt.eventfarm.customcontroller.MyLightingController
import me.jameshunt.eventfarm.device.ezohum.AtlasScientificEzoHumController
import me.jameshunt.eventfarm.vpd.VPDController
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.util.*

fun main() {
    DI
}

// TODO: controller for pressure pump on a timer, that also uses the depth sensor to ensure pump is not run when tank is empty
// TODO: controller for alerts when tank is getting empty
// TODO: controller for dispensing H2O2 automatically proportional to the amount of water left in the tank
// TODO: controller for allowing air to escape from the line (pump would stop working, probably due to H2O2 releasing air bubbles)
// TODO: controller for alerts when humidifier bucket is getting empty (need a sonar sensor on the bucket too)

object DI {
    val configurable = mutableListOf<Configurable>()
    val libDirectory = File("libs").also {
        if (!it.exists()) {
            it.mkdir()
        }
    }
    val moshi = Moshi.Builder()
        .add(object {
            @FromJson
            fun fromJson(json: String): UUID = UUID.fromString(json)

            @ToJson
            fun toJson(value: UUID): String = value.toString()
        })
        .add(object {
            @FromJson
            fun fromJson(json: String): LocalTime = LocalTime.parse(json)

            @ToJson
            fun toJson(value: LocalTime): String = value.toString()
        })
        .add(KotlinJsonAdapterFactory()).build()

    val loggerFactory = LoggerFactory()
    val inputEventManager: InputEventManager = InputEventManager(loggerFactory) { it.getConfigurable() }

    val scheduler: Scheduler = Scheduler(loggerFactory) { findId ->
        val configurable = findId.getConfigurable()
        configurable as? Schedulable ?: throw IllegalArgumentException("configurable is not schedulable: $configurable")
    }

    private val configurableFactory =
        ConfigurableFactory(loggerFactory, moshi, libDirectory, inputEventManager, scheduler)

    init {
        configJson
            .map { configurableFactory.configurableFromJson(it) }
            .let { configurable.addAll(it) }

        configurable
            .mapNotNull { it as? Input }
            .forEach { inputEventManager.addInput(it) }

        val vpdControllerSchedule = ScheduleItem(
            id = configurable.first { it is VPDController }.config.id,
            data = TypedValue.None,
            startTime = Instant.now(),
            endTime = null, // Instant.now().plusSeconds(20) // can have the controller run for a limited amount of time
            index = null
        )
        scheduler.schedule(vpdControllerSchedule)

//        val ecPhControllerId = configurable.schedulable().first { it is ECPHExclusiveLockController }.config.id
//        val ecPhControllerSchedule = Scheduler.ScheduleItem(ecPhControllerId, TypedValue.None, Instant.now(), endTime)
//        scheduler.schedule(ecPhControllerSchedule)

        val ezoHumControllerSchedule = ScheduleItem(
            id = configurable.first { it is AtlasScientificEzoHumController }.config.id,
            data = TypedValue.None,
            startTime = Instant.now(),
            endTime = null,
            index = null
        )
        scheduler.schedule(ezoHumControllerSchedule)

        val myLightingControllerSchedule = ScheduleItem(
            id = configurable.first { it is MyLightingController }.config.id,
            data = TypedValue.None,
            startTime = Instant.now(),
            endTime = null,
            index = null
        )
        scheduler.schedule(myLightingControllerSchedule)
    }

    private fun UUID.getConfigurable(): Configurable {
        return configurable.firstOrNull { it.config.id == this }
            ?: throw IllegalStateException("Configurable not found: $this")
    }
}


// TODO: some sort of safe delete for configurable stuff. make sure there are no dependencies on it,
//  maybe create an DAG graph
