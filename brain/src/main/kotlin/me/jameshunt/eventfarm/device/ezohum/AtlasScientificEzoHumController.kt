package me.jameshunt.eventfarm.device.ezohum

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.Scheduler
import me.jameshunt.eventfarm.core.TypedValue
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * In this case a controller delegates scheduling of sensor values, instead of the input handling it internally
 * TODO: would have to send an mqtt command and get a response, but thats not done
 */
class AtlasScientificEzoHumController(
    override val config: Config,
    private val scheduler: Scheduler
) : Configurable, Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val ezoHumInputId: UUID,
    ) : Configurable.Config

    override fun listenForSchedule(onSchedule: Observable<Scheduler.ScheduleItem>): Disposable {
        return onSchedule.switchMap {
            when {
                it.isStarting -> handle()
                it.isEnding -> Observable.empty()
                else -> Observable.error(IllegalStateException("Should not be possible"))
            }
        }.subscribe({}, { throw it })
    }

    private fun handle(): Observable<Long> {
        return Observable.interval(0, 15, TimeUnit.SECONDS).doOnNext { _ ->
            // TODO?
//            val now = Instant.now()
//            val collectDataScheduleItem = Scheduler.ScheduleItem(config.ezoHumInputId, null, TypedValue.None, now, null)
//            scheduler.schedule(collectDataScheduleItem)
        }
    }
}
