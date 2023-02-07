package me.stroiker.flexibleloadgenerator

import me.stroiker.flexibleloadgenerator.api.LoadGeneratorJob
import me.stroiker.flexibleloadgenerator.config.LoadGeneratorProperties
import me.stroiker.flexibleloadgenerator.domain.LoadProfile
import me.stroiker.flexibleloadgenerator.domain.LoadTask
import me.stroiker.flexibleloadgenerator.mvc.model.*
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class LoadGenerator(private val properties: LoadGeneratorProperties, private val job: LoadGeneratorJob) {

    private var es = Executors.newFixedThreadPool(properties.threadPoolSize)
    private val fixedDelayMillis = 1000L

    @Volatile
    private var isRunning = false
    private var currentTaskId: String? = null
    private lateinit var progressHolderSink: FluxSink<LoadGeneratorProgressResponse>
    private val progressHolder = Flux.create<LoadGeneratorProgressResponse> { sink -> progressHolderSink = sink }

    private val lock = ReentrantLock()

    fun start(request: LoadGeneratorStartRequest): LoadGeneratorStatusResponse {
        lock.withLock {
            if (isRunning) {
                throw IllegalStateException("Load generator already running")
            }
            if (es.isShutdown || es.isTerminated) {
                es = Executors.newFixedThreadPool(properties.threadPoolSize)
            }
            return LoadProfile(request.segments.values, request.timeScale!!).let { loadProfile ->
                LoadTask(loadProfile).also { loadTask ->
                    currentTaskId = loadTask.taskId
                    isRunning = true
                    es.submit {
                        kotlin.runCatching {
                            job.onStart()
                            var taskList = mutableListOf<FutureTask<*>>()
                            loadTask.phases.forEachIndexed { idx, phase ->
                                val segmentStartTimestamp = System.currentTimeMillis()
                                var processedSegmentJobCount: Long = 0
                                for (i in 1..phase.segmentTimes) {
                                    val tasksRemaining = taskList.size
                                    val invokeTimes = (phase.ops - tasksRemaining).takeIf { it > 0 } ?: 0
                                    for (j in 0 until invokeTimes) {
                                        FutureTask { job.onEach() }.also { task ->
                                            taskList.add(task); es.submit(task)
                                        }
                                    }
                                    // calculate expected delay
                                    (segmentStartTimestamp + fixedDelayMillis * i - System.currentTimeMillis())
                                        .takeIf { delay -> delay > 0 }?.also { delay ->
                                            Thread.sleep(delay)
                                        }
                                    taskList = taskList.filter { task -> !task.isDone }.toMutableList()
                                    processedSegmentJobCount += tasksRemaining - taskList.size + invokeTimes
                                }
                                progressHolderSink.next(
                                    LoadGeneratorSegmentStatsResponse(
                                        segmentNumber = idx + 1,
                                        estimatedOPS = phase.ops,
                                        actualOPS = processedSegmentJobCount / phase.segmentTimes,
                                        processedOperations = processedSegmentJobCount
                                    )
                                )
                            }
                            // await finishing jobs
                            taskList.forEach { it.get(10, TimeUnit.SECONDS) }
                            job.onFinish()
                            isRunning = false
                            progressHolderSink.next(LoadGeneratorProgressResponse(true))
                        }.onFailure {
                            isRunning = false
                            throw it
                        }
                    }
                }.let {
                    LoadGeneratorStatusResponse(isRunning, currentTaskId)
                }
            }
        }
    }

    fun stop(): LoadGeneratorStopResponse {
        lock.withLock {
            if (!isRunning)
                throw IllegalStateException("No active task found")
            es.shutdownNow()
            if (!es.awaitTermination(10, TimeUnit.SECONDS))
                throw RuntimeException("Cannot terminate executor")
            val stoppedTaskId = currentTaskId!!
            currentTaskId = null
            isRunning = false
            return LoadGeneratorStopResponse(
                isRunning = isRunning,
                taskId = stoppedTaskId,
            )
        }
    }

    fun getStatus() = LoadGeneratorStatusResponse(isRunning, currentTaskId)

    fun getProgress() = progressHolder.map { stats ->
        ServerSentEvent.builder<LoadGeneratorProgressResponse>()
            .id(currentTaskId!!)
            .event("progress")
            .data(stats)
            .build()
    }
}