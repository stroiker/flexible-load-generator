package me.stroiker.flexibleloadgenerator.generator

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import me.stroiker.flexibleloadgenerator.api.LoadGeneratorJob
import me.stroiker.flexibleloadgenerator.config.LoadGeneratorProperties
import me.stroiker.flexibleloadgenerator.domain.LoadSegment
import me.stroiker.flexibleloadgenerator.domain.LoadTask
import me.stroiker.flexibleloadgenerator.domain.LoadTaskJobResult
import me.stroiker.flexibleloadgenerator.domain.LoadTaskSegmentSummary
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorProgressResponse
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorSegmentStatsResponse
import me.stroiker.flexibleloadgenerator.utils.calculateTm90
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean

internal class LoadGenerator(private val properties: LoadGeneratorProperties, private val job: LoadGeneratorJob) {

    private val isRunning = AtomicBoolean()
    private val executorInitialized = AtomicBoolean()
    private var es = initExecutorService(properties.threadPoolSize)
    private lateinit var progressHolderSink: FluxSink<LoadGeneratorProgressResponse>
    private var progressHolder = initProgressHolder()

    fun start(loadTask: LoadTask) {
        es = initExecutorService(properties.threadPoolSize)
        progressHolder = initProgressHolder()
        isRunning.set(true)
        es.submit {
            kotlin.runCatching {
                job.onStart()
                var taskList = mutableListOf<FutureTask<LoadTaskJobResult>>()
                loadTask.segments.forEachIndexed segments@ { idx, segment ->
                    val segmentSummary = LoadTaskSegmentSummary()
                    repeat(segment.times) times@ { i ->
                        val tasksRemaining = taskList.size
                        val invokeTimes = (segment.ops - tasksRemaining).takeIf { diff -> diff > 0 } ?: 0
                        repeat(invokeTimes) ops@ {
                            if (!isRunning.get()) return@ops
                            FutureTask {
                                val startTime = System.nanoTime()
                                runCatching { job.onEach() }.getOrElse { false }.let { jobResult ->
                                    LoadTaskJobResult(
                                        startTime = startTime,
                                        endTime = System.nanoTime(),
                                        jobResult = jobResult
                                    )
                                }
                            }.also { task ->
                                taskList.add(task); es.submit(task)
                            }
                        }
                        if (!isRunning.get()) return@times
                        // calculate expected delay
                        delay(segmentSummary.startTimestamp + FIXED_DELAY_MILLIS * (i + 1) - System.currentTimeMillis())
                        taskList = taskList.mapNotNull { task ->
                            if (task.isDone && !task.isCancelled)
                                (task.get() as LoadTaskJobResult).let { result ->
                                    (result.endTime - result.startTime).also { latency ->
                                        if (result.jobResult) {
                                            segmentSummary.successJobCount.incrementAndGet()
                                            segmentSummary.successLatencyList.add(latency)
                                        } else {
                                            segmentSummary.failedJobCount.incrementAndGet()
                                            segmentSummary.failedLatencyList.add(latency)
                                        }
                                    }
                                    null
                                }
                            else
                                task
                        }.toMutableList()
                    }
                    if (!isRunning.get()) return@segments
                    progressHolderSink.next(computeSegmentStats(loadTask.taskId, idx + 1, segment, segmentSummary))
                }
                es.shutdownNow()
                job.onFinish()
                isRunning.set(false)
                progressHolderSink.next(LoadGeneratorProgressResponse(loadTask.taskId, true))
                closeProgressHolder()
            }.onFailure { ex ->
                isRunning.set(false)
                ex.printStackTrace()
                throw ex
            }
        }
    }

    @Suppress("DeferredResultUnused")
    @DelicateCoroutinesApi
    fun stop() {
        isRunning.set(false)
        GlobalScope.async {
            executorInitialized.set(false)
            es.shutdownNow()
        }
    }

    fun progressHolder(): Flux<LoadGeneratorProgressResponse> = progressHolder

    fun isRunning(): Boolean = isRunning.get()

    private fun delay(millis: Long) {
        if (millis > 0)
            runCatching { runBlocking { kotlinx.coroutines.delay(millis) } }
    }

    private fun computeSegmentStats(
        taskId: String,
        segmentNumber: Int,
        segment: LoadSegment,
        segmentSummary: LoadTaskSegmentSummary
    ): LoadGeneratorSegmentStatsResponse =
        LoadGeneratorSegmentStatsResponse(
            taskId = taskId,
            segmentNumber = segmentNumber,
            successOperationsCount = segmentSummary.successJobCount.get(),
            failedOperationsCount = segmentSummary.failedJobCount.get(),
            expectedOps = segment.ops,
            actualOps = (segmentSummary.successJobCount.get() + segmentSummary.failedJobCount.get()) / segment.times,
            tm90SuccessLatencyNano = calculateTm90(segmentSummary.successLatencyList),
            tm90FailedLatencyNano = calculateTm90(segmentSummary.failedLatencyList)
        )

    private fun initExecutorService(threadPoolSize: Int?): ExecutorService =
        (threadPoolSize ?: Runtime.getRuntime().availableProcessors()).let { numberOfThreads ->
            println("Initializing fixed thread pool executor with $numberOfThreads thread pool size")
            Executors.newFixedThreadPool(numberOfThreads).also { executorInitialized.set(true) }
        }

    private fun initProgressHolder() = Flux.create<LoadGeneratorProgressResponse> { sink -> progressHolderSink = sink }

    private fun closeProgressHolder() { progressHolderSink.complete() }

    private companion object {
        private const val FIXED_DELAY_MILLIS = 1000L
    }
}