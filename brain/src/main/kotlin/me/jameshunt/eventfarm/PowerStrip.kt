package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.time.Instant
import java.util.*

class PowerStrip(
    totalWattInput: WattInput,
    totalWattHourInput: WattHourInput,
    channels: List<Channel>
) : Device {

    class Channel(
        val wattInput: WattInput,
        val wattHourInput: WattHourInput,
        val onOffOutput: OnOffOutput
    )

    override val inputs: List<Input> =
        listOf(totalWattInput, totalWattHourInput) + channels.flatMap { listOf(it.wattInput, it.wattHourInput) }
    override val outputs: List<Output> = channels.map { it.onOffOutput }

    class WattInput(override val config: Config) : Input {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String,
            val ip: String,
            val index: Int?
        ) : Configurable.Config

        private val id: UUID = config.id
        override fun getInputEvents(): Observable<Input.InputEvent> {
            // TODO("Not yet implemented")
            val event = Input.InputEvent(id, Instant.now(), TypedValue.Watt(0f))
            return Observable.just(event)
        }
    }

    class WattHourInput(override val config: Config) : Input {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String,
            val ip: String,
            val index: Int?
        ) : Configurable.Config

        private val id: UUID = config.id
        override fun getInputEvents(): Observable<Input.InputEvent> {
            // TODO("Not yet implemented")
            val event = Input.InputEvent(id, Instant.now(), TypedValue.WattHour(0f))
            return Observable.just(event)
        }
    }

    class OnOffOutput(override val config: Config) : Output, Scheduler.Schedulable {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String,
            val ip: String,
            val index: Int
        ) : Configurable.Config

        override val id: UUID = config.id

        private var disposable: Disposable? = null
        override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>) {
            if (disposable.hasInitialized()) return

            disposable = onSchedule.subscribe({
                val requestedState = (it.data as? TypedValue.Bool)?.value
                    ?: throw IllegalArgumentException("expected Bool")

                when {
                    it.isStarting -> setState(requestedState)
                    it.isEnding -> setState(!requestedState)
                }
            }, { throw it })
        }

        private fun setState(on: Boolean) {
            println("set state: $on")
            // TODO()
        }
    }
}
