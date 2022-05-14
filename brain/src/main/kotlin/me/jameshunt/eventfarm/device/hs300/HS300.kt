package me.jameshunt.eventfarm.device.hs300

import com.squareup.moshi.Moshi
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.toObservable
import me.jameshunt.eventfarm.core.*
import java.io.File
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

// device wrapper class is unused, but groups the related configurables
class HS300 private constructor() {
    class OnOffOutput(
        override val config: Config,
        private val logger: Logger,
        libDirectory: File,
        moshi: Moshi
    ) : Output, Scheduler.Schedulable {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String,
            val ip: String
        ) : Configurable.Config

        private val hS300Lib: HS300Lib = HS300Lib(libDirectory, moshi)

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

        private fun setState(on: Boolean, index: Int) {
            logger.debug("Set state: $on, plugIndex: $index")
            hS300Lib.setState(config.ip, index, on)
        }
    }

    class Inputs(
        override val config: Config,
        private val logger: Logger,
        libDirectory: File,
        moshi: Moshi
    ) : Input {
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

        private val hS300Lib: HS300Lib = HS300Lib(libDirectory, moshi)

        override fun getInputEvents(): Observable<Input.InputEvent> {
            return Observable.merge(getStateEvents(), getEnergyMeterEvents())
        }

        private fun getStateEvents(): Observable<Input.InputEvent> {
            return Observable
                .interval(0, 10, TimeUnit.SECONDS)
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
                .interval(10, 120, TimeUnit.SECONDS)
                .flatMap {
                    (0 until config.numPlugs)
                        .map { plugIndex -> hS300Lib.getCurrentEnergyMeter(config.ip, plugIndex) }
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
}

