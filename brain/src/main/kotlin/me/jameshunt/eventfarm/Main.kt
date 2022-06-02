package me.jameshunt.eventfarm

import me.jameshunt.eventfarm.core.DI
import java.io.Closeable

fun main(args: Array<String>) {
    DI
    Runtime.getRuntime().addShutdownHook(Thread {
        println("closing resources")
        DI.mqttManager.close()
        DI.configurable
            .mapNotNull { it as? Closeable }
            .forEach { it.close() }
    })
}