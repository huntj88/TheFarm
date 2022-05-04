package me.jameshunt.eventfarm.customcontroller

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.Scheduler
import me.jameshunt.eventfarm.core.TypedValue
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * makes sure that EC and PH probes are not measured at same time, since that would affect the results
 */
class ECPHExclusiveLockController(
    override val config: Config,
    private val scheduler: Scheduler
) : Configurable, Scheduler.Schedulable {
    data class Config(
        override val id: UUID,
        override val className: String,
        // TODO: using humidity and temperature ids + indexes for ph and ec in json
        val ecInputId: UUID,
        val phInputId: UUID
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
        return Observable.interval(0, 60, TimeUnit.SECONDS).doOnNext { _ ->
            val now = Instant.now()
            val switchTime = now.plusSeconds(30)
            val endTime = switchTime.plusSeconds(30)

            val ecScheduleItem = Scheduler.ScheduleItem(config.ecInputId, null, TypedValue.None, now, switchTime)
            val phScheduleItem = Scheduler.ScheduleItem(config.phInputId, null, TypedValue.None, switchTime, endTime)

            scheduler.schedule(ecScheduleItem)
            scheduler.schedule(phScheduleItem)
        }
    }
}