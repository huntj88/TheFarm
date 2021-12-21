package me.jameshunt.thefarm

import me.jameshunt.brain.sql.LogQueries

class PowerManager(private val logQueries: LogQueries) {
    private val powerStripIp: String = "0.0.0.0" // TODO

    enum class PlugAlias(val id: String) {
        Lights("TODO Lights"),
        Water("TODO Water")
    }

    fun setState(plugAlias: PlugAlias, on: Boolean) {
        logQueries.insert("POWER", "$plugAlias, setting state: $on")
//        setStateCommand(plugAlias, on)
    }

    private fun setStateCommand(plugAlias: PlugAlias, on: Boolean) {
        val state = if (on) 1 else 0
        val setChildPlugStateJson = """
            {
                "context":{"child_ids":["${plugAlias.id}"]}, 
                "system":{"set_relay_state":{"state":$state}}
            }
            """.trimIndent()

        "./tplink_smartplug.py -t $powerStripIp -j $setChildPlugStateJson".exec()
            //.also { logQueries.insert("POWER", "Response: $it") }
    }
}
