package me.jameshunt.eventfarm

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

class Scheduler(private val getSchedulable: (UUID) -> Schedulable) {
    data class ScheduleItem(
        val id: UUID, // id of thing that will be scheduled
        val data: TypedValue,
        val startTime: Instant,
        val endTime: Instant?
    ) {
        val isStarting: Boolean
            get() = endTime == null || Instant.now() < endTime

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

    // TODO: what if i drop self schedulable, and instead made device its own controller,
    // TODO: or even control multiple devices that maybe need an exclusive lock on a resource
    // (like ec and ph probes on the water reservoir)
    fun addSelfSchedulable(schedulable: SelfSchedulable) {
        synchronized(this) {
            val scheduleForId = scheduleStream.filter { it.id == schedulable.id }.subscribeOn(Schedulers.io())
            schedulable.listenForSchedule(scheduleForId)
            waiting.add(ScheduleWrapper(schedulable, schedulable.scheduleNext(null)))
        }
    }

    // TODO: create lock on each thing to be schedule using its id
    // TODO: lock is to prevent conflicting commands
    // example: two commands several seconds apart. (first turns on, second turns on, first turns off, second turns off)
    // this is bad because it should probably be something more like (first turns on, second turns off), and drop the middle states

    private val scheduleStream = PublishSubject.create<ScheduleItem>()
    fun loop() = Executors.newSingleThreadExecutor().execute {
        while (true) {
            val waiting = waiting
            val running = running
            val now = Instant.now()
            val starting = waiting.takeWhile { it.scheduleItem.startTime <= now }
            waiting.removeAll(starting)
//            println(starting)
            starting.forEach { scheduleStream.onNext(it.scheduleItem) }

            val withoutEnd = starting.filter { it.scheduleItem.endTime == null }
            val endable = starting.filter { it.scheduleItem.endTime != null }
            running.addAll(endable)
            running.sortBy { it.scheduleItem.endTime!! }
            val ending = running.takeWhile { it.scheduleItem.endTime!! <= now }
            running.removeAll(ending)
            ending.forEach { scheduleStream.onNext(it.scheduleItem) }

            val newScheduleItems = (withoutEnd + ending).mapNotNull {
                if (it.schedulable !is SelfSchedulable) return@mapNotNull null
                it.schedulable to it.scheduleItem
            }.map { (schedulable, scheduleItem) ->
                ScheduleWrapper(schedulable, schedulable.scheduleNext(previousCompleted = scheduleItem))
            }
            waiting.addAll(newScheduleItems)
            waiting.sortBy { it.scheduleItem.startTime }

            Thread.sleep(1000) // TODO: much faster loop time
            // todo: check elapsed time to ensure scheduling thread is never blocked unless flag set or something for debugging
        }
    }
//        .toCompletable().subscribe(
//        { throw IllegalStateException("should not ever complete") },
//        { throw it }
//    )
}
