package me.jameshunt.eventfarm

import me.jameshunt.eventfarm.core.DI
import me.jameshunt.eventfarm.core.DefaultLogger
import me.jameshunt.eventfarm.core.Logger
import java.io.Closeable

fun main(args: Array<String>) {
    DI
    Runtime.getRuntime().addShutdownHook(Thread {
        val logger: Logger = DefaultLogger("ShutdownHook")
        val closeable = listOf<Closeable>(DI.scheduler, DI.inputEventManager, DI.mqttManager) +
            DI.configurable.mapNotNull { it as? Closeable }

        logger.debug("closing resources")
        closeable.forEach {
            try {
                logger.debug("closing $it")
                it.close()
            } catch (e: Exception) {
                logger.error("Could not close $it", e)
            }
        }
    })
}