package me.jameshunt.thefarm

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import me.jameshunt.brain.sql.Database
import me.jameshunt.brain.sql.LogQueries
import java.io.File
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY) // TODO: in memory should be on disk
    Database.Schema.create(driver)
    val database = Database(driver)

    val timer = ScheduledThreadPoolExecutor(4)
    val logger = FarmLogger(database.logQueries)
    val powerManager = PowerManager(logger)

    val lightScheduler = LightScheduler(timer, powerManager, logger)
    val waterScheduler = WaterScheduler(timer, powerManager, logger)
    val photoScheduler = PhotoScheduler(timer, logger)

    lightScheduler.schedule()
    waterScheduler.schedule()
    photoScheduler.schedule()

    // infrequent or only at start unless fancy equipment
    // ph sensor - on solution
    // electrical conductivity sensor - on solution

    // frequent reading
    // humidity sensor
    // pressure sensor
    // light sensor
    // temp sensor
    // photo
    // power draw sensor
    // reservoir level sensor
}

class FarmLogger(private val logQueries: LogQueries) {
    fun info(tag: String, description: String) {
        val time = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        logQueries.insert(time = time, level = "info", tag = tag, description = description, exception = null)
        println("$time -- info --$tag\n$description")
    }

    fun error(tag: String, description: String, e: Exception) {
        val time = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        logQueries.insert(time = time, level = "error", tag = tag, description = description, exception = e.stackTraceToString())
        println("$time -- error --$tag\n$description\n${e.stackTraceToString()}")
        // TODO: maybe push notification?
    }
}

val libDirectory = File("libs").also {
    if (!it.exists()) {
        it.mkdir()
    }
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