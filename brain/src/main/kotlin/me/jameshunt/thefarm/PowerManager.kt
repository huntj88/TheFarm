package me.jameshunt.thefarm

import me.jameshunt.thefarm.PowerManager.PlugAlias.Companion.id
import java.io.File

class PowerManager(private val logger: TypedFarmLogger<PowerManager>) {
    private val cliDirectory: File = File(libDirectory, "tplink-smartplug")
    // todo: env var or autodiscovery
    private val powerStripIp: String = "192.168.1.82"

    init {
        when(cliDirectory.exists()) {
            true -> logger.info("cli already installed")
            false -> {
                logger.info("installing cli")
                // https so no auth required
                "git clone https://github.com/huntj88/tplink-smartplug.git".exec(libDirectory)
            }
        }
    }

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
            logger.info("Command: $setState, Response: $response")
        } catch (e: Exception) {
            logger.error("Command: $setState", e)
            throw e
        }
    }

    private fun String.executeJsonCommand(): String {
        return "./tplink_smartplug.py -t $powerStripIp -j $this".exec(baseDir = cliDirectory)
    }
}
