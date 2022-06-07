package me.jameshunt.eventfarm.core

import java.io.Closeable

object ShutdownHandler {
    /**
     * will close app scope closeables in the order they are provided, as well as any configurables
     */
    fun setup(vararg appCloseables: Closeable, getCloseableConfigurables: () -> List<Closeable>) {
        Runtime.getRuntime().addShutdownHook(Thread {
            val logger: Logger = DefaultLogger(this::class.java.simpleName)
            val closeable = appCloseables.toList() + getCloseableConfigurables()
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
}