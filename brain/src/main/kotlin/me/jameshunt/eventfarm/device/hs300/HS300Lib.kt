package me.jameshunt.eventfarm.device.hs300

import com.squareup.moshi.Moshi
import me.jameshunt.thefarm.exec
import java.io.File

class HS300Lib(
    private val libDirectory: File,
    private val moshi: Moshi
) {
    data class State(val system: System)
    data class System(val get_sysinfo: SystemInfo)
    data class SystemInfo(val deviceId: String, val children: List<PlugState>)
    data class PlugState(val id: String, val state: Int) {
        val index: Int
            get() = id.takeLast(1).toInt()
    }

    class EnergyMeter(val emeter: EnergyMeterChild)
    class EnergyMeterChild(val get_realtime: PlugEnergyMeter)
    class PlugEnergyMeter(
        val slot_id: Int,
        val current_ma: Int,
        val voltage_mv: Int,
        val power_mw: Int,
        val total_wh: Int,
        val err_code: Int
    )

    private val idPrefix = "8006D4C79A1D2CE0935A5A79B28D00291F06E0D10" // TODO parametrize?
    fun setState(ip: String, index: Int, on: Boolean) {
        val state = if (on) 1 else 0
        val setState = """{"context":{"child_ids":["$idPrefix$index"]},"system":{"set_relay_state":{"state":$state}}}"""

        // TODO: uncomment
//        setState.executeJsonCommand(ip)
        when (index) {
            4 -> println("plug with index: $index is disabled")
            0, 1, 2, 3, 5 -> setState.executeJsonCommand(ip)
        }
    }

    fun getCurrentState(ip: String): SystemInfo {
        val parsedData = """{"system":{"get_sysinfo":null}}"""
            .executeJsonCommand(ip)
            .substringAfter("Received:  ")
            .let { moshi.adapter(State::class.java).fromJson(it) }
            ?: throw IllegalStateException("could not parse energy meter json")

        return parsedData.system.get_sysinfo
    }

    fun getCurrentEnergyMeter(ip: String, index: Int): PlugEnergyMeter {
        val getEnergyCommand = """{"emeter":{"get_realtime":{}},"context":{"child_ids":["$idPrefix$index"]}}"""

        val parsedData = getEnergyCommand
            .executeJsonCommand(ip)
            .substringAfter("Received:  ")
            .let { moshi.adapter(EnergyMeter::class.java).fromJson(it) }
            ?: throw IllegalStateException("could not parse energy meter json")

        return parsedData.emeter.get_realtime
    }

    private fun String.executeJsonCommand(ip: String): String {
        return "./tplink-smartplug/tplink_smartplug.py -t $ip -j $this".exec(baseDir = libDirectory)
    }
}
