package me.jameshunt.eventfarm.device.hs300

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.toObservable
import io.reactivex.rxjava3.subjects.PublishSubject
import me.jameshunt.eventfarm.core.*
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
                        TypedValue.WattHour(it.total_wh.toFloat())
                    ),
                )
            }

            return stateEvents.mergeWith(eMeterEvents)
        }
    }
}

