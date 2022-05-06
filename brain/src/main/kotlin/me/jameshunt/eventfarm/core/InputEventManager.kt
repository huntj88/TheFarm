package me.jameshunt.eventfarm.core

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.*

interface IInputEventManager {
    fun getEventStream(): Observable<Input.InputEvent>
}

class InputEventManager(
    private val loggerFactory: LoggerFactory,
    private val getConfigurable: (UUID) -> Configurable
) : IInputEventManager {
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
        getEventStream().subscribe({ inputEvent ->
            val config = getConfigurable.invoke(inputEvent.inputId).config
            val logger = loggerFactory.create(config)
            logger.trace(inputEvent.toString())
        }, { throw it })
    }
}
