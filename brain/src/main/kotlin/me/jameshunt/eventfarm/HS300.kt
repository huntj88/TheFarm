package me.jameshunt.eventfarm

import com.squareup.moshi.Moshi
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.toObservable
import io.reactivex.rxjava3.subjects.PublishSubject
import me.jameshunt.thefarm.exec
import java.io.File
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

// device wrapper class is unused, but groups the related configurables
class HS300 private constructor() {
    class OnOffOutput(
        override val config: Config,
        private val logger: Logger,
        private val hS300Lib: HS300Lib
    ) : Output, Scheduler.Schedulable {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String,
            val ip: String
        ) : Configurable.Config

        override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
            return onSchedule.subscribe({
                val index = it.index ?: throw IllegalArgumentException("plug index required for hs300")
                val requestedState = (it.data as? TypedValue.Bool)?.value
                    ?: throw IllegalArgumentException("expected Bool")

                when {
                    it.isStarting -> setState(requestedState, index)
                    it.isEnding -> setState(!requestedState, index)
                }
            }, { throw it })
        }

        private fun setState(on: Boolean, index: Int) {
            logger.debug("Set state: $on, plugIndex: $index")
            hS300Lib.setState(config.ip, index, on)
        }
    }

    // TODO: make schedulable and use input data to trigger state or eMeter?
    // TODO: could also split up inputs into separate state and emeter inputs?
    class Inputs(override val config: Config, private val hS300Lib: HS300Lib) : Input {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String,
            val ip: String,
            val numPlugs: Int = 6,
            val stateUpdateIntervalSeconds: Int = 10,
            val eMeterUpdateIntervalSeconds: Int = 120
//        val idPrefix: String // TODO?
        ) : Configurable.Config

        private val state = PublishSubject.create<HS300Lib.SystemInfo>()
        private val eMeter = PublishSubject.create<HS300Lib.PlugEnergyMeter>()

        init {
            Observable.interval(0, 10, TimeUnit.SECONDS).subscribe(
                { state.onNext(hS300Lib.getCurrentState(config.ip)) },
                { throw it }
            )

            Observable.interval(10, 120, TimeUnit.SECONDS).subscribe(
                {
                    (0 until config.numPlugs).forEach { plugIndex ->
                        eMeter.onNext(hS300Lib.getCurrentEnergyMeter(config.ip, plugIndex))
                    }
                },
                { throw it }
            )
        }

        override fun getInputEvents(): Observable<Input.InputEvent> {
            val stateEvents = state.flatMap {
                it.children
                    .map { plug ->
                        Input.InputEvent(
                            config.id,
                            plug.index,
                            Instant.now(),
                            TypedValue.Bool(plug.state == 1)
                        )
                    }
                    .toObservable()
            }

            val eMeterEvents = eMeter.flatMap {
                Observable.just(
                    Input.InputEvent(
                        config.id,
                        it.slot_id,
                        Instant.now(),
                        TypedValue.Watt(it.power_mw / 1000F)
                    ),
                    Input.InputEvent(
                        config.id,
                        it.slot_id,
                        Instant.now(),
                        TypedValue.Voltage(it.voltage_mv / 1000F)
                    ),
                    Input.InputEvent(
                        config.id,
                        it.slot_id,
                        Instant.now(),
                        TypedValue.WattHour(it.total_wh.toFloat())
                    ),
                )
            }

            return stateEvents.mergeWith(eMeterEvents)
        }
    }
}

class HS300Lib(
    private val cliDirectory: File,
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
        val setState =
            """{"context":{"child_ids":["${idPrefix + index.toString()}"]},"system":{"set_relay_state":{"state":$state}}}"""

        // TODO: uncomment
//        setState.executeJsonCommand(ip)
    }

    fun getCurrentState(ip: String): SystemInfo {
        val parsedData = """{"system":{"get_sysinfo":null}}"""
            .executeJsonCommand(ip)
            .substringAfter("Received:  ")
//            .let { it.substring(it.indexOf("{")) }
            .also { println("response: $it") }
            .let { moshi.adapter(State::class.java).fromJson(it) }
            ?: throw IllegalStateException("could not parse energy meter json")

        return parsedData.system.get_sysinfo
    }

    fun getCurrentEnergyMeter(ip: String, index: Int): PlugEnergyMeter {
        val getEnergyCommand =
            """{"emeter":{"get_realtime":{}},"context":{"child_ids":["${idPrefix + index.toString()}"]}}"""

        val parsedData = getEnergyCommand
            .executeJsonCommand(ip)
            .substringAfter("Received:  ")
            .let { moshi.adapter(EnergyMeter::class.java).fromJson(it) }
            ?: throw IllegalStateException("could not parse energy meter json")

        return parsedData.emeter.get_realtime
    }

    private fun String.executeJsonCommand(ip: String): String {
        return "./tplink-smartplug/tplink_smartplug.py -t $ip -j $this".exec(baseDir = cliDirectory)
    }
}
