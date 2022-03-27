package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

interface IInputEventManager {
    fun getEventStream(): Observable<Input.InputEvent>
}

class InputEventManager : IInputEventManager {
    private val eventStream = PublishSubject.create<Input.InputEvent>()

    fun addInput(input: Input) {
        // TODO: disposables
        val disposable = input.getInputEvents().subscribe({ eventStream.onNext(it) }, { throw it })
    }

    override fun getEventStream(): Observable<Input.InputEvent> = eventStream
}