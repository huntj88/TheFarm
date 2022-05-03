package me.jameshunt.eventfarm

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.jameshunt.eventfarm.Scheduler.Schedulable
import me.jameshunt.eventfarm.Scheduler.ScheduleItem
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.util.*

fun main() {
    DI
}

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
    val hS300Lib = HS300Lib(libDirectory, moshi)
    val inputEventManager: InputEventManager = InputEventManager(DefaultLogger("InputEventManager"))

    val scheduler: Scheduler = Scheduler(loggerFactory) { findId ->
        configurable.first { it.config.id == findId } as? Schedulable ?: throw IllegalStateException()
    }

    private val configurableFactory = ConfigurableFactory(moshi, inputEventManager, scheduler, hS300Lib, loggerFactory)

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
}


// TODO: some sort of safe delete for configurable stuff. make sure there are no dependencies on it,
//  maybe create an DAG graph
