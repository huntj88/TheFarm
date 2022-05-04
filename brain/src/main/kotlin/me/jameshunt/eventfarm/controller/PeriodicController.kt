package me.jameshunt.eventfarm.controller

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.*
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * two [PeriodicController] could mimic the [AtlasScientificEzoHumController], but not the [ECPHExclusiveLockController]
 **/

class PeriodicController(
    override val config: Config,
    private val scheduler: Scheduler
) : Configurable, Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val schedulableId: UUID,
        val periodMillis: Long,
        val durationMillis: Long?
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
        return Observable.interval(0, config.periodMillis, TimeUnit.MILLISECONDS).doOnNext { _ ->
            val now = Instant.now()
            val end = config.durationMillis?.let { now.plusMillis(it) }
            scheduler.schedule(Scheduler.ScheduleItem(config.schedulableId, null, TypedValue.None, now, end))
        }
    }
}