package me.jameshunt.eventfarm.core

import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

/**
 * Returns true if a lazy property reference has been initialized, or if the property is not lazy.
 */
val KProperty0<*>.isLazyInitialized: Boolean
    get() {
        if (this !is Lazy<*>) return true

        // Prevent IllegalAccessException from JVM access check on private properties.
        val originalAccessLevel = isAccessible
        isAccessible = true
        val isLazyInitialized = (getDelegate() as Lazy<*>).isInitialized()
        // Reset access level.
        isAccessible = originalAccessLevel
        return isLazyInitialized
    }

fun String.exec(baseDir: File = File(Path.of("").toAbsolutePath().toString())): String {
    val process = ProcessBuilder().directory(baseDir).command(split(" ").filter { it != " " })
        .start()!!
        .also { it.waitFor(20, TimeUnit.SECONDS) }

    try {
        process.exitValue()
    } catch (e: IllegalThreadStateException) {
        throw Exception("command timed out", e)
    }

    if (process.exitValue() != 0) {
        throw Exception(process.errorStream.bufferedReader().readText())
    }
    return process.inputStream.bufferedReader().readText().trim()
}