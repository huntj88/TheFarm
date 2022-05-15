package me.jameshunt.eventfarm

import me.jameshunt.eventfarm.core.ConfigurableFactory
import me.jameshunt.eventfarm.core.DI
import org.junit.jupiter.api.Test

internal class ConfigurableFactoryTest {

//    @Test
//    fun test() {
//        val di = DI
//        di.configurable.forEach {
//            val configSerializer = ConfigurableFactory(
//                di.moshi,
//                di.inputEventManager,
//                di.scheduler,
//                di.hS300Lib,
//                di.loggerFactory
//            )
//            configSerializer.serialize(it).also { configSerializer.configurableFromJson(it) }
//        }
//    }
//
//    @Test
//    fun test2() {
//        val di = DI
//        val config =
//            """{"id":"00000000-0000-0000-0000-000000000003","className":"me.jameshunt.eventfarm.PowerStrip${'$'}WattInput${'$'}Config","name":"Total watts being used for all devices","ip":"192.168.1.82"}"""
//        val configSerializer = ConfigurableFactory(
//            di.moshi,
//            di.inputEventManager,
//            di.scheduler,
//            di.hS300Lib,
//            di.loggerFactory
//        )
//        println(configSerializer.configurableFromJson(config))
//    }
}