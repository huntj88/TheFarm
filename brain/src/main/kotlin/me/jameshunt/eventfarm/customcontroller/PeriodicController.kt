package me.jameshunt.eventfarm.customcontroller

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.Scheduler
import me.jameshunt.eventfarm.core.TypedValue
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import me.jameshunt.eventfarm.device.ezohum.AtlasScientificEzoHumController

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
        val schedulableIndex: Int?,
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
            // TODO: input data?
            scheduler.schedule(Scheduler.ScheduleItem(config.schedulableId, config.schedulableIndex, TypedValue.None, now, end))
        }
    }
}