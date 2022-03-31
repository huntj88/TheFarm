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

    interface Schedulable : Configurable {
        fun listenForSchedule(onSchedule: Observable<ScheduleItem>): Disposable
    }

    private data class ScheduleWrapper(
        val schedulable: Schedulable,
        val scheduleItem: ScheduleItem
    )

    private val waiting: LinkedList<ScheduleWrapper> = LinkedList()
    private val running: LinkedList<ScheduleWrapper> = LinkedList()

    private val scheduleStream = PublishSubject.create<ScheduleItem>()
    private val streamListeners = mutableMapOf<UUID, Disposable>()

    init {
        // todo: disposable? or future?
        loop()
    }

    fun schedule(item: ScheduleItem) {
        val schedulable = getSchedulable(item.id)

        val disposable = streamListeners[item.id]
        if (disposable == null || disposable.isDisposed) {
            streamListeners[item.id] = schedulable.listenForSchedule(scheduleStreamFor(schedulable.config.id))
        }
        synchronized(waiting) {
            waiting.add(ScheduleWrapper(schedulable, item))
        }
    }

    // TODO: create lock on each thing to be schedule using its id
    // TODO: lock is to prevent conflicting commands
    // example: two commands several seconds apart. (first turns on, second turns on, first turns off, second turns off)
    // this is bad because it should probably be something more like (first turns on, second turns off), and drop the middle states

    // TODO: Or instead of doing the above todo: log warnings to review

    private fun loop() = Executors.newSingleThreadExecutor().execute {
        while (true) {
            val now = Instant.now()
            val starting = waiting.takeWhile { it.scheduleItem.startTime <= now }
            waiting.removeAll(starting)
            println(starting)
            starting.forEach { scheduleStream.onNext(it.scheduleItem) }

            val endable = starting.filter { it.scheduleItem.endTime != null }
            running.addAll(endable)
            running.sortBy { it.scheduleItem.endTime!! }
            val ending = running.takeWhile { it.scheduleItem.endTime!! <= now }
            running.removeAll(ending)
            ending.forEach { scheduleStream.onNext(it.scheduleItem) }
            waiting.sortBy { it.scheduleItem.startTime }

            Thread.sleep(1000) // TODO: much faster loop time
            // todo: check elapsed time to ensure scheduling thread is never blocked unless flag set or something for debugging
        }
    }
//        .toCompletable().subscribe(
//        { throw IllegalStateException("should not ever complete") },
//        { throw it }
//    )

    private fun scheduleStreamFor(id: UUID): Observable<ScheduleItem> {
        return scheduleStream.filter { it.id == id }.subscribeOn(Schedulers.io())
    }
}
