package me.jameshunt.eventfarm

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalTime
import java.util.*

val listOfJson = listOf(
    """{"id":"00000000-0000-0000-0000-000000000003","className":"me.jameshunt.eventfarm.HS300${'$'}WattInput${'$'}Config","name":"Total watts being used for all devices","ip":"192.168.1.82"}""",
    """{"id":"00000000-0000-0000-0000-000000000004","className":"me.jameshunt.eventfarm.HS300${'$'}WattHourInput${'$'}Config","name":"total watt hours used for all devices","ip":"192.168.1.82"}""",
    """{"id":"00000000-0000-0000-0000-000000000100","className":"me.jameshunt.eventfarm.HS300${'$'}WattInput${'$'}Config","name":"Total watts being used for all devices","ip":"192.168.1.82","index":0}""",
    """{"id":"00000000-0000-0000-0000-000000000101","className":"me.jameshunt.eventfarm.HS300${'$'}WattHourInput${'$'}Config","name":"total watt hours used for all devices","ip":"192.168.1.82","index":0}""",
    """{"id":"00000000-0000-0000-0000-000000000110","className":"me.jameshunt.eventfarm.HS300${'$'}WattInput${'$'}Config","name":"Total watts being used for all devices","ip":"192.168.1.82","index":1}""",
    """{"id":"00000000-0000-0000-0000-000000000111","className":"me.jameshunt.eventfarm.HS300${'$'}WattHourInput${'$'}Config","name":"total watt hours used for all devices","ip":"192.168.1.82","index":1}""",
    """{"id":"00000000-0000-0000-0000-000000000120","className":"me.jameshunt.eventfarm.HS300${'$'}WattInput${'$'}Config","name":"Total watts being used for all devices","ip":"192.168.1.82","index":2}""",
    """{"id":"00000000-0000-0000-0000-000000000121","className":"me.jameshunt.eventfarm.HS300${'$'}WattHourInput${'$'}Config","name":"total watt hours used for all devices","ip":"192.168.1.82","index":2}""",
    """{"id":"00000000-0000-0000-0000-000000000130","className":"me.jameshunt.eventfarm.HS300${'$'}WattInput${'$'}Config","name":"Total watts being used for all devices","ip":"192.168.1.82","index":3}""",
    """{"id":"00000000-0000-0000-0000-000000000131","className":"me.jameshunt.eventfarm.HS300${'$'}WattHourInput${'$'}Config","name":"total watt hours used for all devices","ip":"192.168.1.82","index":3}""",
    """{"id":"00000000-0000-0000-0000-000000000140","className":"me.jameshunt.eventfarm.HS300${'$'}WattInput${'$'}Config","name":"Total watts being used for all devices","ip":"192.168.1.82","index":4}""",
    """{"id":"00000000-0000-0000-0000-000000000141","className":"me.jameshunt.eventfarm.HS300${'$'}WattHourInput${'$'}Config","name":"total watt hours used for all devices","ip":"192.168.1.82","index":4}""",
    """{"id":"00000000-0000-0000-0000-000000000150","className":"me.jameshunt.eventfarm.HS300${'$'}WattInput${'$'}Config","name":"Total watts being used for all devices","ip":"192.168.1.82","index":5}""",
    """{"id":"00000000-0000-0000-0000-000000000151","className":"me.jameshunt.eventfarm.HS300${'$'}WattHourInput${'$'}Config","name":"total watt hours used for all devices","ip":"192.168.1.82","index":5}""",
    """{"id":"00000000-0000-0000-0000-000000000005","className":"me.jameshunt.eventfarm.AtlasScientificEzoHum${'$'}TemperatureInput${'$'}Config","name":"temp"}""",
    """{"id":"00000000-0000-0000-0000-000000000006","className":"me.jameshunt.eventfarm.AtlasScientificEzoHum${'$'}HumidityInput${'$'}Config","name":"humidity"}""",
    """{"id":"00000000-0000-0000-0000-000000000007","className":"me.jameshunt.eventfarm.VPDFunction${'$'}Config","temperatureId":"00000000-0000-0000-0000-000000000005","humidityId":"00000000-0000-0000-0000-000000000006"}""",
    """{"id":"00000000-0000-0000-0000-000000000102","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffOutput${'$'}Config","name":"turn plug on or off at position: 0","ip":"192.168.1.82","index":0}""",
    """{"id":"00000000-0000-0000-0000-000000000112","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffOutput${'$'}Config","name":"turn plug on or off at position: 1","ip":"192.168.1.82","index":1}""",
    """{"id":"00000000-0000-0000-0000-000000000122","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffOutput${'$'}Config","name":"turn plug on or off at position: 2","ip":"192.168.1.82","index":2}""",
    """{"id":"00000000-0000-0000-0000-000000000132","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffOutput${'$'}Config","name":"turn plug on or off at position: 3","ip":"192.168.1.82","index":3}""",
    """{"id":"00000000-0000-0000-0000-000000000142","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffOutput${'$'}Config","name":"turn plug on or off at position: 4","ip":"192.168.1.82","index":4}""",
    """{"id":"00000000-0000-0000-0000-000000000152","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffOutput${'$'}Config","name":"turn plug on or off at position: 5","ip":"192.168.1.82","index":5}""",
    """{"id":"00000000-0000-0000-0000-000000000202","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffInput${'$'}Config","name":"get current on/off state at position: 0","ip":"192.168.1.82","index":0}""",
    """{"id":"00000000-0000-0000-0000-000000000212","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffInput${'$'}Config","name":"get current on/off state at position: 1","ip":"192.168.1.82","index":1}""",
    """{"id":"00000000-0000-0000-0000-000000000222","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffInput${'$'}Config","name":"get current on/off state at position: 2","ip":"192.168.1.82","index":2}""",
    """{"id":"00000000-0000-0000-0000-000000000232","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffInput${'$'}Config","name":"get current on/off state at position: 3","ip":"192.168.1.82","index":3}""",
    """{"id":"00000000-0000-0000-0000-000000000242","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffInput${'$'}Config","name":"get current on/off state at position: 4","ip":"192.168.1.82","index":4}""",
    """{"id":"00000000-0000-0000-0000-000000000252","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffInput${'$'}Config","name":"get current on/off state at position: 5","ip":"192.168.1.82","index":5}""",
    """{"id":"00000000-0000-0000-0001-000000000000","className":"me.jameshunt.eventfarm.VPDController${'$'}Config","vpdInputId":"00000000-0000-0000-0000-000000000007","humidifierOutputId":"00000000-0000-0000-0000-000000000152"}""",
//    """{"id":"00000000-0000-0000-0002-000000000000","className":"me.jameshunt.eventfarm.ECPHExclusiveLockController${'$'}Config","ecInputId":"00000000-0000-0000-0000-000000000005","phInputId":"00000000-0000-0000-0000-000000000006"}""",
    """{"id":"00000000-0000-0000-0002-000000000000","className":"me.jameshunt.eventfarm.AtlasScientificEzoHumController${'$'}Config","humidityInputId":"00000000-0000-0000-0000-000000000005","temperatureInputId":"00000000-0000-0000-0000-000000000006"}""",
    """{"id":"00000000-0000-0000-0003-000000000000","className":"me.jameshunt.eventfarm.HS300InputController${'$'}Config","totalWattInputId":"00000000-0000-0000-0000-000000000003","totalWattHourInputId":"00000000-0000-0000-0000-000000000004","index0OnOffInputId":"00000000-0000-0000-0000-000000000202","index1OnOffInputId":"00000000-0000-0000-0000-000000000212","index2OnOffInputId":"00000000-0000-0000-0000-000000000222","index3OnOffInputId":"00000000-0000-0000-0000-000000000232","index4OnOffInputId":"00000000-0000-0000-0000-000000000242","index5OnOffInputId":"00000000-0000-0000-0000-000000000252","index0WattInputId":"00000000-0000-0000-0000-000000000100","index1WattInputId":"00000000-0000-0000-0000-000000000110","index2WattInputId":"00000000-0000-0000-0000-000000000120","index3WattInputId":"00000000-0000-0000-0000-000000000130","index4WattInputId":"00000000-0000-0000-0000-000000000140","index5WattInputId":"00000000-0000-0000-0000-000000000150","index0WattHourInputId":"00000000-0000-0000-0000-000000000101","index1WattHourInputId":"00000000-0000-0000-0000-000000000111","index2WattHourInputId":"00000000-0000-0000-0000-000000000121","index3WattHourInputId":"00000000-0000-0000-0000-000000000131","index4WattHourInputId":"00000000-0000-0000-0000-000000000141","index5WattHourInputId":"00000000-0000-0000-0000-000000000151"}""",
    """{"id":"00000000-0000-0000-0004-000000000000","className":"me.jameshunt.eventfarm.MyLightingController${'$'}Config","lightOnOffInputId":"00000000-0000-0000-0000-000000000202","lightOnOffOutputId":"00000000-0000-0000-0000-000000000102","turnOnTime":"03:00","turnOffTime":"16:00"}"""
)


class ConfigurableFactory(
    inputEventManager: IInputEventManager,
    scheduler: Scheduler,
    private val loggerFactory: LoggerFactory
) {

    private val injectableComponents: Map<String, Any> = mapOf(
        IInputEventManager::class.java.name to inputEventManager,
        Scheduler::class.java.name to scheduler
    )

    // at some point i might have to add a migration step if I rename configurable class names or locations

    private val moshi = Moshi.Builder()
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

    fun serialize(configurable: Configurable): String {
        val clazz = Class.forName(configurable.config.className)

        val adapter = moshi.adapter<Configurable.Config>(clazz)
        return adapter.toJson(configurable.config).also { println(it) }
    }

    // used to deserialize any configurable.config with a concrete class
    private data class Hack(override val id: UUID, override val className: String) : Configurable.Config

    fun configurableFromJson(json: String): Configurable {
        // get information the interface gives us
        val data = moshi.adapter(Hack::class.java).fromJson(json)
        val className = data?.className ?: throw IllegalArgumentException()

        // reparse the json with the real adapter
        val typedAdapter = moshi.adapter<Configurable.Config>(Class.forName(className))
        val config = typedAdapter.fromJson(json) ?: throw IllegalStateException()

        val configurable = config::class.java.enclosingClass
        check(Configurable::class.java.isAssignableFrom(configurable))

        return createInjected(configurable, config)
    }

    private fun <T> createInjected(classToInject: Class<T>, config: Configurable.Config): Configurable {
        val constructor = classToInject.constructors.first()
        val args = constructor.parameters.map {
            if (it.type.isAssignableFrom(config::class.java)) {
                return@map config
            }

            if (it.type == Logger::class.java) {
                return@map loggerFactory.create(config)
            }

            val canonicalName = it.type.canonicalName!!
            injectableComponents[canonicalName] ?: throw IllegalStateException(
                """
                $canonicalName has not been registered, 
                and cannot be injected into ${classToInject.canonicalName!!}
                """.trimIndent()
            )
        }

        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(*args.toTypedArray()) as Configurable
    }
}
