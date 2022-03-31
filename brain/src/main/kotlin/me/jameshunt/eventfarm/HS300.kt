package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.time.Instant
import java.util.*

// TODO: played around with new approach to devices. Just a reference for all the individual inputs and output of a device
//data class HS300(
//    val id: UUID,
//    val totalWattInputId: UUID,
//    val totalWattHourInputId: UUID,
//
//    val index0WattInputId: UUID,
//    val index1WattInputId: UUID,
//    val index2WattInputId: UUID,
//    val index3WattInputId: UUID,
//    val index4WattInputId: UUID,
//    val index5WattInputId: UUID,
//
//    val index0WattHourInputId: UUID,
//    val index1WattHourInputId: UUID,
//    val index2WattHourInputId: UUID,
//    val index3WattHourInputId: UUID,
//    val index4WattHourInputId: UUID,
//    val index5WattHourInputId: UUID,
//
//    val index0OnOffOutputId: UUID,
//    val index1OnOffOutputId: UUID,
//    val index2OnOffOutputId: UUID,
//    val index3OnOffOutputId: UUID,
//    val index4OnOffOutputId: UUID,
//    val index5OnOffOutputId: UUID,
//)

// device wrapper class is unused, but groups the related configurables
class HS300(
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

    class OnOffOutput(override val config: Config, private val logger: Logger) : Output, Scheduler.Schedulable {
        data class Config(
            override val id: UUID,
            override val className: String = Config::class.java.name,
            val name: String,
            val ip: String,
            val index: Int
        ) : Configurable.Config

        override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
            return onSchedule.subscribe({
                val requestedState = (it.data as? TypedValue.Bool)?.value
                    ?: throw IllegalArgumentException("expected Bool")

                when {
                    it.isStarting -> setState(requestedState)
                    it.isEnding -> setState(!requestedState)
                }
            }, { throw it })
        }

        private fun setState(on: Boolean) {
            logger.debug("Set state: $on, plugIndex: ${config.index}")
            // TODO()
        }
    }
}
