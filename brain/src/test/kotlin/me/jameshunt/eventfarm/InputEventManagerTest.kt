package me.jameshunt.eventfarm

import io.reactivex.rxjava3.observers.TestObserver
import org.junit.jupiter.api.Test
import java.util.*

internal class InputEventManagerTest {

    @Test
    fun test() {
//        val testObserver = TestObserver<Input.InputEvent>()
//        val inputs = createAtlasScientficiEzoHum().inputs
//        val output = createPowerStrip().outputs.find {
//            it.config.id == "00000000-0000-0000-0000-000000000152".let {
//                UUID.fromString(it)
//            }
//        }!!
//        val iem = InputEventManager()
//        iem.getEventStream().doOnNext { println(it) }.subscribe(testObserver)
//
//        inputs.forEach { iem.addInput(it) }
//        iem.addInput(getVPDInput(iem))

//        val vpdController = getVPDController(sch, iem)
//        val vpdControllerId = vpdController.id
//
//        val sch = Scheduler {
//            when (it) {
//                vpdControllerId -> vpdController
//                output.id -> output
//                else -> throw IllegalArgumentException()
//            }
//        }.apply {
//            inputs.mapNotNull { it as? Scheduler.SelfSchedulable }.forEach { addSelfSchedulable(it) }
//            loop()
//        }
//
//        val endTime = null // Instant.now().plusSeconds(20) // can have the controller run for a limited amount of time
//        val controllerSchedule = Scheduler.ScheduleItem(vpdControllerId, TypedValue.None, Instant.now(), endTime)
//        sch.schedule(controllerSchedule)

//        sch.loop()
//        testObserver.await()
    }

//    private fun createPowerStrip(): Device {
//        // in the future serialize power strip to json and put in sqlite
//        // retrieve device json and restore on startup
//
//        // for now just hardcode the known settings
//        val ip = "192.168.1.82"
//
//        fun createChannel(index: Int): HS300.Channel {
//            return HS300.Channel(
//                wattInput = HS300.WattInput(
//                    HS300.WattInput.Config(
//                        name = "Total watts being used for all devices",
//                        id = "00000000-0000-0000-0000-0000000001${index}0".let { UUID.fromString(it) },
//                        ip = ip,
//                        index = index
//                    )
//                ),
//                wattHourInput = HS300.WattHourInput(
//                    HS300.WattHourInput.Config(
//                        name = "total watt hours used for all devices",
//                        id = "00000000-0000-0000-0000-0000000001${index}1".let { UUID.fromString(it) },
//                        ip = ip,
//                        index = index
//                    )
//                ),
//                onOffOutput = let {
//                    val config = HS300.OnOffOutput.Config(
//                        name = "turn plug on or off at position: $index",
//                        id = "00000000-0000-0000-0000-0000000001${index}2".let { UUID.fromString(it) },
//                        ip = ip,
//                        index = index
//                    )
//                    HS300.OnOffOutput(
//                        config,
//                        LoggerFactory().create(config)
//                    )
//                }
//            )
//        }
//
//        return HS300(
//            totalWattInput = HS300.WattInput(
//                HS300.WattInput.Config(
//                    "00000000-0000-0000-0000-000000000003".let { UUID.fromString(it) },
//                    HS300.WattInput.Config::class.java.name,
//                    "Total watts being used for all devices",
//                    ip,
//                    null
//                )
//            ),
//            totalWattHourInput = HS300.WattHourInput(
//                HS300.WattHourInput.Config(
//                    "00000000-0000-0000-0000-000000000004".let { UUID.fromString(it) },
//                    HS300.WattHourInput.Config::class.java.name,
//                    "total watt hours used for all devices",
//                    ip,
//                    null
//                )
//            ),
//            channels = (0..5).map { createChannel(it) }
//        )
//    }


    private fun getVPDInput(inputEventManager: InputEventManager): Input {
        val vpdInputId = "00000000-0000-0000-0000-000000000007".let { UUID.fromString(it) }
        val tempInputId = "00000000-0000-0000-0000-000000000005".let { UUID.fromString(it) }
        val humidityInputId = "00000000-0000-0000-0000-000000000006".let { UUID.fromString(it) }
        return VPDFunction(
            VPDFunction.Config(vpdInputId, temperatureId = tempInputId, humidityId = humidityInputId),
            inputEventManager
        )
    }
}