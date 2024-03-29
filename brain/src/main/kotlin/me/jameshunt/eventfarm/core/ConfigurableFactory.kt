package me.jameshunt.eventfarm.core

import com.squareup.moshi.Moshi
import java.io.File
import java.util.*

// TODO: verify no duplicate when creating or reading?
// TODO: create a map of id's to configurable names, so i can display it in logging?
class ConfigurableFactory(
    private val loggerFactory: LoggerFactory,
    private val moshi: Moshi,
    libDirectory: File,
    inputEventManager: IInputEventManager,
    scheduler: Scheduler,
    mqttManager: MQTTManager,
) {

    private val injectableComponents: Map<String, Any> = mapOf(
        Moshi::class.java.name to moshi,
        File::class.java.name to libDirectory,
        IInputEventManager::class.java.name to inputEventManager,
        Scheduler::class.java.name to scheduler,
        MQTTManager::class.java.name to mqttManager,
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

        try {
            val data = moshi.adapter(Hack::class.java).fromJson(json)
            val className = data?.className ?: throw IllegalArgumentException()

            // reparse the json with the real adapter
            val typedAdapter = moshi.adapter<Configurable.Config>(Class.forName(className))
            val config = typedAdapter.fromJson(json) ?: throw IllegalStateException()

            val configurable = config::class.java.enclosingClass
            check(Configurable::class.java.isAssignableFrom(configurable))

            loggerFactory.create(config).debug(
                message = "injecting configurable with json: $json",
            )

            return createInjected(configurable, config)
        } catch (e: Throwable) {
            DefaultLogger(ConfigurableFactory::class.java.simpleName).error(
                message = "error injecting configurable with json: $json",
                throwable = e
            )
            throw e
        }
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
