package me.jameshunt.eventfarm.controller

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.Configurable
import me.jameshunt.eventfarm.Scheduler
import me.jameshunt.eventfarm.TypedValue
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

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
            val now = Instant.now()
            val collectDataScheduleItem = Scheduler.ScheduleItem(config.ezoHumInputId, null, TypedValue.None, now, null)
            scheduler.schedule(collectDataScheduleItem)
        }
    }
}
