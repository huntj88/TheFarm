package me.jameshunt.eventfarm.device

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.Logger
import me.jameshunt.eventfarm.core.Scheduler
import me.jameshunt.eventfarm.core.exec
import java.io.Closeable
import java.util.*

class AndroidCamera(
    override val config: Config,
    private val logger: Logger
) : Scheduler.Schedulable, Closeable { // TODO mqtt input
    data class Config(
        override val id: UUID,
        override val className: String = Config::class.java.name,
        val name: String,
//        val mqttTopic: String,
    ) : Configurable.Config

    init {
        logger.debug("Starting ADB")
        "adb start-server".exec()
    }

//    override fun getInputEvents(): Observable<Input.InputEvent> {
//        // TODO:
//        return mqttManager
//            .listen(config.mqttTopic)
//            .flatMap { parseMessage(it) }
//    }

    override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
        return onSchedule.doOnNext {
            if (it.isStarting) {
                // TODO: if errors out, check if adb is starting, and try again
                "bash takePicture.sh".exec()
            }
        }.subscribe({}, { logger.error("could not schedule picture", it) })
    }

    override fun close() {
        "adb kill-server".exec()
    }
}