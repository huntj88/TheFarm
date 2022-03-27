package me.jameshunt.eventfarm

import io.reactivex.rxjava3.observers.TestObserver
import org.junit.jupiter.api.Test

internal class InputEventManagerTest {


//    @Test
//    fun test() {
//        val testObserver = TestObserver<Input.InputEvent>()
//        val inputs = createAtlasScientficiEzoHum().inputs
//        InputEventManager(inputs).getEventStream().doOnNext { println(it) }.subscribe(testObserver)
//
//        Scheduler({ throw IllegalStateException("$it")}).apply {
//            inputs.mapNotNull { it as? Scheduler.SelfSchedulable }.forEach { addSelfSchedulable(it) }
//            loop()
//        }
//        testObserver.await()
//    }
}