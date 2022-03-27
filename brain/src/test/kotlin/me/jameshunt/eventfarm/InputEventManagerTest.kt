package me.jameshunt.eventfarm

import io.reactivex.rxjava3.observers.TestObserver
import org.junit.jupiter.api.Test
import java.util.*

internal class InputEventManagerTest {

    @Test
    fun test() {
        val testObserver = TestObserver<Input.InputEvent>()
        val inputs = createAtlasScientficiEzoHum().inputs
        val output = createPowerStrip().outputs.find { it.id == "00000000-0000-0000-0000-000000000152".let { UUID.fromString(it) } }!!
        val iem = InputEventManager()
        iem.getEventStream().doOnNext { println(it) }.subscribe(testObserver)

        inputs.forEach { iem.addInput(it) }
        iem.addInput(getVPDInput { iem })

        val sch = Scheduler({output}).apply {
            inputs.mapNotNull { it as? Scheduler.SelfSchedulable }.forEach { addSelfSchedulable(it) }
            loop()
        }

        val blah = getVPDController(sch, iem).handle()

//        sch.loop()
        testObserver.await()
    }
}