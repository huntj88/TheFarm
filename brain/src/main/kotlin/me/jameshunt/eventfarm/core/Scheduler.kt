package me.jameshunt.eventfarm.core

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.io.Closeable
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// TODO: persist scheduled stuff to sqlite? resume a schedule when rebooted?
//  like putting H2O2 in the water 3 days after the last time. controller scheduling the h2o2 could check the last time it was scheduled for after reboot?
class Scheduler(
    private val logger: Logger,
    private val loggerFactory: LoggerFactory,
    private val getSchedulable: (UUID) -> Schedulable
) : Closeable {
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

    private val executor = Executors.newSingleThreadExecutor()

    private val waiting: LinkedList<ScheduleWrapper> = LinkedList()
    private val running: LinkedList<ScheduleWrapper> = LinkedList()

    private val scheduleStream = PublishSubject.create<ScheduleItem>()
    private val streamListeners = mutableMapOf<UUID, Disposable>()

    @Volatile
    private var keepRunning = true

    init {
        loop()
    }

    fun schedule(item: ScheduleItem) {
        val schedulable = getSchedulable(item.id)

        synchronized(this) {
            val disposable = streamListeners[item.id]
            if (disposable == null || disposable.isDisposed) {
                streamListeners[item.id] = schedulable.listenForSchedule(scheduleStreamFor(schedulable.config.id))
            }
            waiting.add(ScheduleWrapper(schedulable, item))
        }
    }

    override fun close() {
        logger.debug("shutting down scheduler")
        keepRunning = false
        executor.awaitTermination(10, TimeUnit.SECONDS)
        streamListeners.forEach { (_, disposable) -> disposable.dispose() }
        scheduleStream.onComplete()
    }

    private fun loop() = executor.execute {
        while (true) {
            synchronized(this) {
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
            }

            Thread.sleep(50)
            // todo: check elapsed time to ensure scheduling thread is never blocked unless flag set or something for debugging
        }
    }

    private fun scheduleStreamFor(id: UUID): Observable<ScheduleItem> {
        return scheduleStream.filter { it.id == id }.subscribeOn(Schedulers.io())
    }
}
