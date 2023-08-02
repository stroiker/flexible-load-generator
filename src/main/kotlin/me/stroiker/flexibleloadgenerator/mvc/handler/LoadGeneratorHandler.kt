package me.stroiker.flexibleloadgenerator.mvc.handler

import kotlinx.coroutines.DelicateCoroutinesApi
import me.stroiker.flexibleloadgenerator.domain.LoadProfile
import me.stroiker.flexibleloadgenerator.domain.LoadTask
import me.stroiker.flexibleloadgenerator.generator.LoadGenerator
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorProgressResponse
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorResponse
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorStartRequest
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class LoadGeneratorHandler(private val loadGenerator: LoadGenerator) {

    private val lock = ReentrantLock()

    fun start(request: LoadGeneratorStartRequest): LoadGeneratorResponse =
        runCatching {
            lock.withLock {
                if (loadGenerator.isRunning())
                    throw IllegalStateException("Load generator already running")
                LoadProfile(request.segments.values, request.timeScale!!).let { loadProfile ->
                    LoadTask(loadProfile).let { loadTask ->
                        loadGenerator.start(loadTask)
                        LoadGeneratorResponse(loadGenerator.isRunning())
                    }
                }
            }
        }.getOrElse { it.printStackTrace(); throw it }

    @DelicateCoroutinesApi
    fun stop(): LoadGeneratorResponse {
        lock.withLock {
            if (loadGenerator.isRunning())
                loadGenerator.stop()
            return LoadGeneratorResponse(loadGenerator.isRunning())
        }
    }

    fun status() = LoadGeneratorResponse(loadGenerator.isRunning())

    fun progress(): Flux<ServerSentEvent<LoadGeneratorProgressResponse>> =
        loadGenerator.progressHolder().map { stats ->
            ServerSentEvent.builder<LoadGeneratorProgressResponse>()
                .id(stats.taskId)
                .event("progress")
                .data(stats)
                .build()
        }
}