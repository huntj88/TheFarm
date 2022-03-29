package me.jameshunt.eventfarm

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.*


class ConfigurableFactory(private val injectableComponents: Map<String, Any>) {

    private val moshi = Moshi.Builder()
        .add(object {
        @FromJson
        fun fromJson(json: String): UUID {
            return UUID.fromString(json)
        }

        @ToJson
        fun toJson(uuid: UUID): String {
            return uuid.toString()
        }

    }).add(KotlinJsonAdapterFactory()).build()

    fun serialize(configurable: Configurable): String {
        println(configurable.config.className)
        println(configurable.config.className)
        val clazz = Class.forName(configurable.config.className)

        val adapter = moshi.adapter<Configurable.Config>(clazz)
        return adapter.toJson(configurable.config).also { println(it) }
    }

    // used to deserialize any configurable.config with a concrete class
    private data class Hack(override val id: UUID, override val className: String): Configurable.Config

    fun deserialize(json: String) {
        val data = moshi.adapter(Hack::class.java).fromJson(json)
        val className = data?.className ?: throw IllegalArgumentException()
        val clazz = Class.forName(className)
        val typedAdapter = moshi.adapter<Configurable.Config>(clazz)
        val config = typedAdapter.fromJson(json)!!
        println(config)

        val configurable = config::class.java.enclosingClass
        println(configurable)
        check(Configurable::class.java.isAssignableFrom(configurable))

        createInjected(configurable, config)
    }

    fun <T> createInjected(classToInject: Class<T>, config: Configurable.Config): T {
        val constructor = classToInject.constructors.first()
        val args = constructor.parameters.map {
            if (it.type.isAssignableFrom(config::class.java)) {
                return@map config
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
        return constructor.newInstance(*args.toTypedArray()) as T
    }
}