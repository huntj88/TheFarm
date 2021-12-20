package me.jameshunt.thefarm

import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {
    val timer = Timer()
    val lightManager = LightManager()
    val photoManager = PhotoManager()

    lightManager.schedule(timer)
    photoManager.schedule(timer)

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

fun String.log() {
    val time = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    println("$time\n$this")
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