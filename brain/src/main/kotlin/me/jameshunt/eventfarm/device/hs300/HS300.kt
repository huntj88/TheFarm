package me.jameshunt.eventfarm.device.hs300

import com.squareup.moshi.Moshi
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.toObservable
import me.jameshunt.eventfarm.core.*
import java.io.Closeable
import java.io.File
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

// TODO: set up default plug states for startup?
class HS300(
    libDirectory: File,
    moshi: Moshi,
    override val config: Config,
    private val logger: Logger
) : Input, Scheduler.Schedulable, Closeable {
    data class Config(
        override val id: UUID,
        override val className: String = Config::class.java.name,
        val name: String,
        val ip: String,
        val deviceIdPrefix: String,
        val numPlugs: Int = 6,
        val stateUpdateIntervalSeconds: Int = 20, // TODO: move to json
        val eMeterUpdateIntervalSeconds: Int = 120,
        val shutdownState: String = "0,0,0,0,0,0" // 0 for off, 1 for on
//        val idPrefix: String // TODO?
    ) : Configurable.Config

    private val hS300Lib: HS300Lib = HS300Lib(libDirectory, moshi)

    override fun getInputEvents(): Observable<Input.InputEvent> {
        return Observable.merge(getStateEvents(), getEnergyMeterEvents())
    }

    override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
        return onSchedule.subscribe({
            val index = it.index ?: throw IllegalArgumentException("plug index required for hs300")
            val requestedState = (it.data as? TypedValue.Bool)?.value
                ?: throw IllegalArgumentException("expected Bool")

            when {
                it.isStarting -> setState(requestedState, index)
                it.isEnding -> setState(!requestedState, index)
            }
        }, { logger.error("could not set plug state", it) })
    }

    override fun close() {
        logger.debug("setting hs300 shutdown state")
        // set plugs to assigned state when program is shutting down
        config.shutdownState
            .split(",")
            .map { it == "1" }
            .forEachIndexed { i, on ->
                hS300Lib.setState(
                    config.ip,
                    deviceIdPrefix = config.deviceIdPrefix,
                    index = i,
                    on
                )
            }
    }

    private fun setState(on: Boolean, index: Int) {
        logger.debug("Set state: $on, plugIndex: $index")
        hS300Lib.setState(config.ip, config.deviceIdPrefix, index, on)
    }

    private fun getStateEvents(): Observable<Input.InputEvent> {
        return Observable
            .interval(0, config.stateUpdateIntervalSeconds.toLong(), TimeUnit.SECONDS)
            .map { hS300Lib.getCurrentState(config.ip) }
            .flatMap {
                it.children.map { plug ->
                    Input.InputEvent(
                        config.id,
                        plug.index,
                        Instant.now(),
                        TypedValue.Bool(plug.state == 1)
                    )
                }.toObservable()
            }
    }

    private fun getEnergyMeterEvents(): Observable<Input.InputEvent> {
        return Observable
            .interval(10, config.eMeterUpdateIntervalSeconds.toLong(), TimeUnit.SECONDS)
            .flatMap {
                (0 until config.numPlugs)
                    .map { plugIndex -> hS300Lib.getCurrentEnergyMeter(config.ip, config.deviceIdPrefix, plugIndex) }
                    .toObservable()
            }
            .flatMap {
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
                        TypedValue.WattHour(it.total_wh.toFloat())
                    ),
                )
            }
    }
}

