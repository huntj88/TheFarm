package me.jameshunt.thefarm

import me.jameshunt.brain.sql.LogQueries

class PowerManager(private val logQueries: LogQueries) {
    enum class PlugAlias {
        Lights,
        Water
    }

    fun setState(plugAlias: PlugAlias, on: Boolean) {
        logQueries.insert("POWER", "$plugAlias, on: $on")
        // TODO
    }
}