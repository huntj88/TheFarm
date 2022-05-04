package me.jameshunt.eventfarm.core

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.*

interface IInputEventManager {
    fun getEventStream(): Observable<Input.InputEvent>
}

class InputEventManager(private val logger: Logger) : IInputEventManager {
    private val streamListeners = mutableMapOf<UUID, Disposable>()
    private val eventStream = PublishSubject.create<Input.InputEvent>()

    fun addInput(input: Input) {
        val inputId = input.config.id
        val disposable = streamListeners[inputId]
        if (disposable == null || disposable.isDisposed) {
            streamListeners[inputId] = input.getInputEvents().subscribe({ eventStream.onNext(it) }, { throw it })
        }
    }

    override fun getEventStream(): Observable<Input.InputEvent> = eventStream

    init {
        getEventStream().subscribe({ logger.trace(it.toString()) }, { throw it })
    }
}

//class InputEventLogger(inputEventManager: InputEventManager) {
//    init {
//        // TODO: log somewhere
//        inputEventManager.getEventStream().subscribe({ println(it) }, { throw it })
//    }
//}