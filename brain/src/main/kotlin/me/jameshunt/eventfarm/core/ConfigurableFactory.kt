package me.jameshunt.eventfarm.core

import com.squareup.moshi.Moshi
import me.jameshunt.eventfarm.device.hs300.HS300Lib
import java.util.*

// TODO: verify no duplicate when creating or reading?
// TODO: create a map of id's to configurable names, so i can display it in logging?
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
