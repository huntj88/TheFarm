package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

// could be a PID controller or something
class VPDController(
    private val scheduler: Scheduler,
    private val inputEventManager: InputEventManager,
    private val vpdInputId: UUID,
    private val humidifierOutputId: UUID
) {
    fun handle(): Disposable {
        return inputEventManager
            .getEventStream()
            .filter { it.inputId == vpdInputId }
            .filter {
                val vpd = it.value as TypedValue.Pascal
                vpd.value > 925
            }
            .subscribe {
                val startTime = Instant.now()
                val endTime = startTime.plusSeconds(5)
                scheduler.schedule(
                    Scheduler.ScheduleItem(
                        humidifierOutputId,
                        TypedValue.Bool(true),
                        startTime,
                        endTime
                    )
                )
            }
    }
}

class Scheduler(private val getSchedulable: (UUID) -> Schedulable) {
    data class ScheduleItem(
        val id: UUID, // id of thing that will be scheduled
        val data: TypedValue,
        val startTime: Instant,
        val endTime: Instant?
    ) {
        val isStarting: Boolean
            get() = Instant.now() < endTime

        val isEnding: Boolean
            get() = endTime != null && Instant.now() >= endTime
    }

    interface Schedulable {
        val id: UUID
        fun listenForSchedule(onSchedule: Observable<ScheduleItem>)
    }

    interface SelfSchedulable : Schedulable {
        fun scheduleNext(previousCompleted: ScheduleItem?): ScheduleItem
    }

    private data class ScheduleWrapper(
        val schedulable: Schedulable,
        val scheduleItem: ScheduleItem
    )

    private val waiting: LinkedList<ScheduleWrapper> = LinkedList()
    private val running: LinkedList<ScheduleWrapper> = LinkedList()

    fun schedule(item: ScheduleItem) {
        synchronized(this) {
            val schedulable = getSchedulable(item.id)
            val scheduleForId = scheduleStream.filter { it.id == schedulable.id }.subscribeOn(Schedulers.io())
            schedulable.listenForSchedule(scheduleForId)
            waiting.add(ScheduleWrapper(schedulable, item))
        }
    }

    fun addSelfSchedulable(schedulable: SelfSchedulable) {
        synchronized(this) {
            val scheduleForId = scheduleStream.filter { it.id == schedulable.id }.subscribeOn(Schedulers.io())
            schedulable.listenForSchedule(scheduleForId)
            waiting.add(ScheduleWrapper(schedulable, schedulable.scheduleNext(null)))
        }
    }

    private val scheduleStream = PublishSubject.create<ScheduleItem>()
    fun loop(): Disposable = Executors.newSingleThreadExecutor().submit {
        while (true) {
            val now = Instant.now()
            val starting = waiting.takeWhile { it.scheduleItem.startTime <= now }
            waiting.removeAll(starting)
            starting.forEach { scheduleStream.onNext(it.scheduleItem) }

            val endable = starting.filter { it.scheduleItem.endTime != null }
            running.addAll(endable)
            running.sortBy { it.scheduleItem.endTime!! }
            val ending = running.takeWhile { it.scheduleItem.endTime!! <= now }
            ending.forEach { scheduleStream.onNext(it.scheduleItem) }

            val newScheduleItems = ending.mapNotNull {
                if (it.schedulable !is SelfSchedulable) return@mapNotNull null
                it.schedulable to it.scheduleItem
            }.map { (schedulable, scheduleItem) ->
                ScheduleWrapper(schedulable, schedulable.scheduleNext(previousCompleted = scheduleItem))
            }
            waiting.addAll(newScheduleItems)
            waiting.sortBy { it.scheduleItem.startTime }

            Thread.sleep(100) // TODO: much faster loop time
        }
    }.toCompletable().subscribe(
        { throw IllegalStateException("should not ever complete") },
        { throw it }
    )
}
