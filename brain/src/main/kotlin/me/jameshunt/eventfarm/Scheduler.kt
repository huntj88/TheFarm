package me.jameshunt.eventfarm

import io.reactivex.rxjava3.kotlin.toCompletable
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

class Scheduler(private val schedulables: List<Schedulable>) {
    data class ScheduleItem(
        val startTime: Instant,
        val endTime: Instant?,
        val onStart: () -> Unit,
        val onEnd: () -> Unit
    )

    interface Schedulable {
        // completed is null if first init
        fun schedule(completed: ScheduleItem?): ScheduleItem
    }

    private data class ScheduleWrapper(
        val schedulable: Schedulable,
        val scheduleItem: ScheduleItem
    )

    private val scheduleLoop = Executors.newSingleThreadExecutor().submit {
        val waiting: LinkedList<ScheduleWrapper> = schedulables
            .map { ScheduleWrapper(it, it.schedule(null)) }
            .sortedBy { it.scheduleItem.startTime }
            .let { LinkedList(it) }

        val running: LinkedList<ScheduleWrapper> = LinkedList()

        while (true) {
            val now = Instant.now()
            val starting = waiting.takeWhile { it.scheduleItem.startTime <= now }
            waiting.removeAll(starting)

            // todo: call on thread pool?
            // todo: or keep this single threaded, but make sure all actions are handled using appropriate rx java scheduler
            starting.forEach { it.scheduleItem.onStart() }

            running.addAll(starting)
            running.sortBy { it.scheduleItem.endTime ?: it.scheduleItem.startTime }
            val ending = running.takeWhile { (it.scheduleItem.endTime ?: it.scheduleItem.startTime) <= now }
            ending.forEach { it.scheduleItem.onEnd() }

            val newScheduleItems = ending.map { it.copy(scheduleItem = it.schedulable.schedule(it.scheduleItem)) }
            waiting.addAll(newScheduleItems)
            waiting.sortBy { it.scheduleItem.startTime }

            Thread.sleep(1000)
        }
    }

    init {
        scheduleLoop.toCompletable().subscribe(
            { throw IllegalStateException("should not ever complete") },
            { throw it }
        )
    }
}
