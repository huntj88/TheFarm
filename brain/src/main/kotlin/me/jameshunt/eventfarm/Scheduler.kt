package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

class Scheduler {
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
        // completed is null if first init
//        fun onScheduled(): Observable<ScheduleItem>
        fun listenForSchedule(onSchedule: Observable<ScheduleItem>)
    }

    interface SelfSchedulable : Schedulable {
        fun schedule(previousCompleted: ScheduleItem?): ScheduleItem
    }

    private data class ScheduleWrapper(
        val schedulable: Schedulable,
        val scheduleItem: ScheduleItem
    )

    private val waiting: LinkedList<ScheduleWrapper> = LinkedList()
    private val running: LinkedList<ScheduleWrapper> = LinkedList()

    fun schedule(schedulable: Schedulable, item: ScheduleItem) {
        synchronized(this) {
            schedulable.listenForSchedule(scheduleStream.filter { it.id == schedulable.id })
            waiting.add(ScheduleWrapper(schedulable, item))
        }
    }

    fun addSelfSchedulable(schedulable: SelfSchedulable) {
        synchronized(this) {
            schedulable.listenForSchedule(scheduleStream.filter { it.id == schedulable.id })
            waiting.add(ScheduleWrapper(schedulable, schedulable.schedule(null)))
        }
    }

    private val scheduleStream = PublishSubject.create<ScheduleItem>()
    fun loop(): Disposable = Executors.newSingleThreadExecutor().submit {
        while (true) {
            val now = Instant.now()
            val starting = waiting.takeWhile { it.scheduleItem.startTime <= now }
            waiting.removeAll(starting)

            // todo: call on thread pool?
            // todo: or keep this single threaded, but make sure all actions are handled using appropriate rx java scheduler
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
                ScheduleWrapper(schedulable, schedulable.schedule(previousCompleted = scheduleItem))
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
