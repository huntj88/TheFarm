package me.jameshunt.eventfarm

import org.junit.jupiter.api.Test

internal class ConfigurableFactoryTest {

    @Test
    fun test() {
        val di = DI()
        (di.inputs + di.outputs + di.vpdController).forEach {
            val configSerializer = ConfigurableFactory(
                injectableComponents = mapOf(
                    IInputEventManager::class.java.name to di.inputEventManager,
                    Scheduler::class.java.name to di.scheduler,
                )
            )
            configSerializer.serialize(it).also { configSerializer.deserialize(it) }
        }
    }
}