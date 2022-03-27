package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.time.Instant
import java.util.*

fun createPowerStrip(): Device {
    // in the future serialize power strip to json and put in sqlite
    // retrieve device json and restore on startup

    // for now just hardcode the known settings
    val ip = "192.168.1.82"

    fun createChannel(index: Int): PowerStrip.Channel {
        return PowerStrip.Channel(
            wattInput = PowerStrip.WattInput(
                PowerStrip.WattInput.Config(
                    name = "Total watts being used for all devices",
                    id = UUID.randomUUID(),
                    ip = ip,
                    index = index
                )
            ),
            wattHourInput = PowerStrip.WattHourInput(
                PowerStrip.WattHourInput.Config(
                    name = "total watt hours used for all devices",
                    id = UUID.randomUUID(),
                    ip = ip,
                    index = index
                )
            ),
            onOffOutput = PowerStrip.OnOffOutput(
                PowerStrip.OnOffOutput.Config(
                    name = "turn plug on or off at position: $index",
                    id = UUID.randomUUID(),
                    ip = ip,
                    index = index
                )
            )
        )
    }

    return PowerStrip(
        totalWattInput = PowerStrip.WattInput(
            PowerStrip.WattInput.Config(
                "Total watts being used for all devices",
                UUID.randomUUID(),
                ip,
                null
            )
        ),
        totalWattHourInput = PowerStrip.WattHourInput(
            PowerStrip.WattHourInput.Config(
                "total watt hours used for all devices",
                UUID.randomUUID(),
                ip,
                null
            )
        ),
        channels = (0..5).map { createChannel(it) }
    )
}

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

    class WattInput(private val config: Config) : Input {
        data class Config(val name: String, val id: UUID, val ip: String, val index: Int?)

        override val id: UUID = config.id
        override fun getInputEvents(): Observable<Input.InputEvent> {
            // TODO("Not yet implemented")
            val event = Input.InputEvent(id, Instant.now(), TypedValue.Watt(0f))
            return Observable.just(event)
        }
    }

    class WattHourInput(private val config: Config) : Input {
        data class Config(val name: String, val id: UUID, val ip: String, val index: Int?)

        override val id: UUID = config.id
        override fun getInputEvents(): Observable<Input.InputEvent> {
            // TODO("Not yet implemented")
            val event = Input.InputEvent(id, Instant.now(), TypedValue.WattHour(0f))
            return Observable.just(event)
        }
    }

    class OnOffOutput(private val config: Config) : Output, Scheduler.Schedulable {
        data class Config(val name: String, val id: UUID, val ip: String, val index: Int)

        override val id: UUID = config.id

        private var disposable: Disposable? = null
        override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>) {
            disposable?.dispose() // TODO: can i not subscribe to the new one and use existing subscription?
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
            TODO()
        }
    }
}
