package me.jameshunt.eventfarm.core

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.jameshunt.eventfarm.core.Scheduler.Schedulable
import me.jameshunt.eventfarm.core.Scheduler.ScheduleItem
import me.jameshunt.eventfarm.customcontroller.MyLightingController
import me.jameshunt.eventfarm.customcontroller.PressurePumpController
import me.jameshunt.eventfarm.customcontroller.WateringController
import me.jameshunt.eventfarm.vpd.VPDController
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.util.*

fun main() {
    DI
}

// TODO: controller for alerts when tank is getting empty
// TODO: controller for dispensing H2O2 automatically proportional to the amount of water left in the tank
//  (and later use dissolved oxygen sensor, which i think might be proportional to h2o2 concentration)
// TODO: controller for alerts when humidifier bucket is getting empty (need a sonar sensor on the bucket too)
// TODO: install script of libs and dependencies. tplink-smartplug, adb (install is different on pi and linux), etc
// TODO: controller for taking timelapse photos,
//  Camera would need to be an input with Input.InputEvent(data=TypedValue.Image(imgId)) if you wanted a record that could be used internally
//  otherwise, camera would just be an output, an action that can be scheduled (more like this at the moment with the way the android app works
//  ensure camera reset on camera app

// TODO: can i re-instantiate an input when it errors due to subject already being disposed?

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

    val mqttManager = MQTTManager(DefaultLogger(MQTTManager::class.java.name))

    private val configurableFactory =
        ConfigurableFactory(loggerFactory, moshi, libDirectory, inputEventManager, scheduler, mqttManager)

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

//        val ezoHumControllerSchedule = ScheduleItem(
//            id = configurable.first { it is AtlasScientificEzoHumController }.config.id,
//            data = TypedValue.None,
//            startTime = Instant.now(),
//            endTime = null,
//            index = null
//        )
//        scheduler.schedule(ezoHumControllerSchedule)

        val myLightingControllerSchedule = ScheduleItem(
            id = configurable.first { it is MyLightingController }.config.id,
            data = TypedValue.None,
            startTime = Instant.now(),
            endTime = null,
            index = null
        )
        scheduler.schedule(myLightingControllerSchedule)

        val wateringControllerSchedule = ScheduleItem(
            id = configurable.first { it is WateringController }.config.id,
            data = TypedValue.None,
            startTime = Instant.now(),
            endTime = null,
            index = null
        )
        scheduler.schedule(wateringControllerSchedule)

        val pressurePumpControllerSchedule = ScheduleItem(
            id = configurable.first { it is PressurePumpController }.config.id,
            data = TypedValue.None,
            startTime = Instant.now(),
            endTime = null,
            index = null
        )
        scheduler.schedule(pressurePumpControllerSchedule)
    }

    private fun UUID.getConfigurable(): Configurable {
        return configurable.firstOrNull { it.config.id == this }
            ?: throw IllegalStateException("Configurable not found: $this")
    }
}


// TODO: some sort of safe delete for configurable stuff. make sure there are no dependencies on it,
//  maybe create an DAG graph
