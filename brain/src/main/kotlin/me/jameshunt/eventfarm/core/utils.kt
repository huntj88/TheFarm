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

fun String.exec(
    baseDir: File = File(Path.of("").toAbsolutePath().toString()),
    timeoutSeconds: Long = 20,
): String {
    val process = ProcessBuilder().directory(baseDir).command(split(" ").filter { it != " " })
        .start()!!
        .also { it.waitFor(timeoutSeconds, TimeUnit.SECONDS) }

    try {
        process.exitValue()
    } catch (e: IllegalThreadStateException) {
        throw Exception("command timed out", e)
    }

    if (process.exitValue() != 0) {
        // TODO: will exit with 143 (SIGTERM) if thefarm service stopped in middle of a command, should it wait for normal completion?
        val errorMessage = process.errorStream.bufferedReader().readText()
        throw Exception("errorCode: ${process.exitValue()}, $errorMessage")
    }
    return process.inputStream.bufferedReader().readText().trim()
}