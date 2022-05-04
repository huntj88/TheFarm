package me.jameshunt.eventfarm

import com.squareup.moshi.Moshi
import java.util.*

val listOfJson = listOf(
    """{"id":"00000000-0000-0000-0000-000000000003","className":"me.jameshunt.eventfarm.HS300${'$'}Inputs${'$'}Config","name":"On/Off Status, and power usage metrics","ip":"192.168.1.82"}""",
    """{"id":"00000000-0000-0000-0000-000000000005","className":"me.jameshunt.eventfarm.AtlasScientificEzoHum${'$'}TemperatureInput${'$'}Config","name":"temp"}""",
    """{"id":"00000000-0000-0000-0000-000000000006","className":"me.jameshunt.eventfarm.AtlasScientificEzoHum${'$'}HumidityInput${'$'}Config","name":"humidity"}""",
    """{"id":"00000000-0000-0000-0000-000000000007","className":"me.jameshunt.eventfarm.VPDFunction${'$'}Config","temperatureId":"00000000-0000-0000-0000-000000000005","humidityId":"00000000-0000-0000-0000-000000000006"}""",
    """{"id":"00000000-0000-0000-0000-000000000102","className":"me.jameshunt.eventfarm.HS300${'$'}OnOffOutput${'$'}Config","name":"turn plug on or off at a position","ip":"192.168.1.82"}""",
    """{"id":"00000000-0000-0000-0003-000000000000","className":"me.jameshunt.eventfarm.DepthSensorInput${'$'}Config","name":"returns distance water is from the sensor, and a percent remaining based on the config depth ","depthOfTankCentimeters":"20","depthWhenFullCentimeters":3}""",

    """{"id":"00000000-0000-0000-0001-000000000000","className":"me.jameshunt.eventfarm.controller.VPDController${'$'}Config","vpdInputId":"00000000-0000-0000-0000-000000000007","humidifierOutputId":"00000000-0000-0000-0000-000000000152","humidifierOutputIndex": 5}""",
//    """{"id":"00000000-0000-0000-0002-000000000000","className":"me.jameshunt.eventfarm.controller.ECPHExclusiveLockController${'$'}Config","ecInputId":"00000000-0000-0000-0000-000000000005","phInputId":"00000000-0000-0000-0000-000000000006"}""",
    """{"id":"00000000-0000-0000-0002-000000000000","className":"me.jameshunt.eventfarm.controller.AtlasScientificEzoHumController${'$'}Config","humidityInputId":"00000000-0000-0000-0000-000000000005","temperatureInputId":"00000000-0000-0000-0000-000000000006"}""",
    """{"id":"00000000-0000-0000-0004-000000000000","className":"me.jameshunt.eventfarm.controller.MyLightingController${'$'}Config","lightOnOffInputId":"00000000-0000-0000-0000-000000000003", "inputIndex": 0,"lightOnOffOutputId":"00000000-0000-0000-0000-000000000102", "outputIndex": 0,"turnOnTime":"03:00","turnOffTime":"16:00"}"""
)

// TODO: verify no duplicate when creating or reading?
class ConfigurableFactory(
    private val moshi: Moshi,
    inputEventManager: IInputEventManager,
    scheduler: Scheduler,
    hS300Lib: HS300Lib,
    private val loggerFactory: LoggerFactory
) {

    private val injectableComponents: Map<String, Any> = mapOf(
        IInputEventManager::class.java.name to inputEventManager,
        Scheduler::class.java.name to scheduler,
        HS300Lib::class.java.name to hS300Lib
    )

    // at some point i might have to add a migration step if I rename configurable class names or locations


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
