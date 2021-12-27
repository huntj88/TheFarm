package me.jameshunt.thefarm

import me.jameshunt.brain.sql.LogQueries
import me.jameshunt.thefarm.PowerManager.PlugAlias.Companion.id
import java.io.File

class PowerManager(private val logQueries: LogQueries) {
    // todo: env var
    private val cliDirectory: File = File("/home/jameshunt/IdeaProjects/tplink-smartplug")

    // todo: env var or autodiscovery
    // private val powerStripIp: String = "192.168.1.82"
    private val powerStripIp: String = "192.168.1.4"

    enum class PlugAlias(private val plugIndex: Int) {
        Water(0),
        Lights(1);

        companion object {
            private const val idPrefix = "8006D4C79A1D2CE0935A5A79B28D00291F06E0D10"
            val PlugAlias.id: String
                get() = "$idPrefix${this.plugIndex}"
        }
    }

    fun setState(plugAlias: PlugAlias, on: Boolean) {
        val state = if (on) 1 else 0
        val setState = """{"context":{"child_ids":["${plugAlias.id}"]},"system":{"set_relay_state":{"state":$state}}}"""
        try {
            val response = setState.executeJsonCommand()
            logQueries.insert(LogLevel.Info, "POWER", "Command: $setState, Response: $response")
        } catch (e: Exception) {
            logQueries.insert(LogLevel.Error, "POWER", "Command: $setState, Response: $e")
        }
    }

    private fun String.executeJsonCommand(): String {
        return "./tplink_smartplug.py -t $powerStripIp -j $this".exec(baseDir = cliDirectory)
    }
}
