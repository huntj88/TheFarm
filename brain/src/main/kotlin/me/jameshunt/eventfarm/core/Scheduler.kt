package me.jameshunt.eventfarm.core

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

class Scheduler(private val loggerFactory: LoggerFactory, private val getSchedulable: (UUID) -> Schedulable) {
    data class ScheduleItem(
        val id: UUID,
        /** If a [Configurable] has multiple inputs or outputs, outputs, index refers to a specific input or output */
        val index: Int?,
        val data: TypedValue,
        val startTime: Instant,
        val endTime: Instant?,
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
        loop()
            .toCompletable()
            .subscribeOn(Schedulers.io())
            .subscribe(
                { throw IllegalStateException("should never complete") },
                { throw it }
            )
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

    private fun loop() = Executors.newSingleThreadExecutor().submit {
        while (true) {
            val now = Instant.now()
            waiting.sortBy { it.scheduleItem.startTime }
            val starting = waiting.takeWhile { it.scheduleItem.startTime <= now }
            waiting.removeAll(starting)
            starting.forEach {
                loggerFactory.create(it.schedulable.config).trace("Starting: ${it.scheduleItem}")
                scheduleStream.onNext(it.scheduleItem)
            }

            val hasEnd = starting.filter { it.scheduleItem.endTime != null }
            running.addAll(hasEnd)
            running.sortBy { it.scheduleItem.endTime!! }
            val ending = running.takeWhile { it.scheduleItem.endTime!! <= now }
            running.removeAll(ending)
            ending.forEach {
                loggerFactory.create(it.schedulable.config).trace("Ending: ${it.scheduleItem}")
                scheduleStream.onNext(it.scheduleItem)
            }

            Thread.sleep(200) // TODO: much faster loop time
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
