package me.jameshunt.eventfarm

import me.jameshunt.eventfarm.core.DI

fun main(args: Array<String>) {
    DI
    Runtime.getRuntime().addShutdownHook(Thread {
        println("shutdownHook")
        // TODO: resource cleanup
    })
}