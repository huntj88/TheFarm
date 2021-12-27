package me.jameshunt.thefarm

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import me.jameshunt.brain.sql.Database
import me.jameshunt.brain.sql.LogQueries
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY) // TODO: in memory should be on disk
    Database.Schema.create(driver)
    val database = Database(driver)

    val timer = Timer()
    val powerManager = PowerManager(database.logQueries)

    val lightScheduler = LightScheduler(powerManager)
    val waterScheduler = WaterScheduler(powerManager)
    val photoScheduler = PhotoScheduler(database.logQueries)

    lightScheduler.schedule(timer)
    waterScheduler.schedule(timer)
    photoScheduler.schedule(timer)

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

enum class LogLevel {
    Info,
    Error
}

fun LogQueries.insert(level: LogLevel, tag: String, description: String) {
    val time = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    this.insert(time = time, level = level.name, tag = tag, description = description)
    println("$time -- $tag\n$description")

    if (level == LogLevel.Error) {
        // TODO: maybe push notification?
    }
}


// TODO: env var
const val mainDir: String = "/home/jameshunt/IdeaProjects/TheFarm"

fun String.exec(baseDir: File = File(mainDir)): String {
    val process = ProcessBuilder().directory(baseDir).command(split(" ").filter { it != " " })
        .start()!!
        .also { it.waitFor(10, TimeUnit.SECONDS) }

    if (process.exitValue() != 0) {
        throw Exception(process.errorStream.bufferedReader().readText())
    }
    return process.inputStream.bufferedReader().readText().trim()
}