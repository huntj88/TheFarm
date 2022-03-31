package me.jameshunt.eventfarm

import org.junit.jupiter.api.Test

internal class ConfigurableFactoryTest {

    @Test
    fun test() {
        val di = DI
        di.configurable.forEach {
            val configSerializer = ConfigurableFactory(
                injectableComponents = mapOf(
                    IInputEventManager::class.java.name to di.inputEventManager,
                    Scheduler::class.java.name to di.scheduler,
                )
            )
            configSerializer.serialize(it).also { configSerializer.configurableFromJson(it) }
        }
    }

    @Test
    fun test2() {
        val config = """{"id":"00000000-0000-0000-0000-000000000003","className":"me.jameshunt.eventfarm.PowerStrip${'$'}WattInput${'$'}Config","name":"Total watts being used for all devices","ip":"192.168.1.82"}"""
        val configSerializer = ConfigurableFactory(
            injectableComponents = emptyMap()
        )
        println(configSerializer.configurableFromJson(config))
    }
}