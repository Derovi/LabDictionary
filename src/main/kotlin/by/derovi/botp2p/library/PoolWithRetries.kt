package by.derovi.botp2p.library

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PoolWithRetries(nThreads: Int) {
    private val executor = Executors.newFixedThreadPool(nThreads)
    private val scheduledCount = AtomicInteger(0)
    private val tasks = mutableListOf<Task>()

    private inner class Task(val retryTimes: Int = 0, val runnable: Runnable): Runnable {
        override fun run() {
            try {
                runnable.run()
                if (retryTimes > 0) {
                    println("retry suc")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                if (retryTimes < 1) {
                    println("retry! $retryTimes")
                    scheduledCount.incrementAndGet()
                    executor.execute(Task(retryTimes + 1, runnable))
                } else {
                    println("ignored!")
                }
            } finally {
                scheduledCount.decrementAndGet()
            }
        }
    }

    fun addTask(task: () -> Unit) {
        scheduledCount.incrementAndGet()
        tasks.add(Task(0, task))
    }

    fun shuffleTasks() {
        tasks.shuffle()
    }

    fun executeTasks() {
        for (task in tasks) {
            executor.execute(task)
        }

        while (!executor.isShutdown) {
            Thread.sleep(100)
            if (scheduledCount.get() == 0) {
                executor.shutdown()
                try {
                    if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                        executor.shutdownNow()
                    }
                } catch (e: InterruptedException) {
                    executor.shutdownNow()
                }
            }
        }
    }
}