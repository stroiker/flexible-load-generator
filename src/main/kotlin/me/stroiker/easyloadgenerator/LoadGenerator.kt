/*
 *
 *  Copyright 2021 @stroiker
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package me.stroiker.easyloadgenerator

import me.stroiker.easyloadgenerator.config.LoadGeneratorProperties
import me.stroiker.easyloadgenerator.mvc.model.LoadGeneratorStatusResponse
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LoadGenerator(private val properties: LoadGeneratorProperties, private val job: LoadGeneratorJob) {

    private var es = Executors.newFixedThreadPool(properties.threadPoolSize)
    private val delayMillis = 1000L

    @Volatile
    private var isRunning = false

    var profile: LoadProfile? = null
        private set

    fun createLoadProfile(phases: List<LoadPhase>) {
        profile = LoadProfile.builder()
            .apply { phases.forEach { phase(it) } }
            .job { times: Long ->
                es.submit { repeat(times.toInt()) { job.onEach() }.also { job.onBatch() } }
                    .also { Thread.sleep(delayMillis) }
            }
            .onEndCallback { job.onEnd().also { isRunning = false } }
            .build()
    }

    fun start() {
        profile?.also {
            if (!isRunning) {
                isRunning = true
                if (es.isTerminated)
                    es = Executors.newFixedThreadPool(properties.threadPoolSize)
                es.submit { it.run() }
            }
        } ?: throw IllegalStateException("Load profile must be created before starting")
    }

    fun getStatus() = LoadGeneratorStatusResponse(if(isRunning) LoadGeneratorState.ON else LoadGeneratorState.OFF)

    fun getProgress() = Flux.interval(Duration.ofSeconds(1))
        .map { sequence: Long ->
            ServerSentEvent.builder<Int>()
                .id(sequence.toString())
                .event("progress")
                .data(profile?.takeIf { isRunning }?.getProgressPercentage() ?: -1)
                .build()
        }

    fun getLoadProfile() = profile?.getLoadProfileInfo(properties.graphHeight, properties.graphWidth)

    fun stop() {
        es.shutdownNow()
        if (!es.awaitTermination(10, TimeUnit.SECONDS))
            throw RuntimeException("Cannot terminate executor")
        isRunning = false
    }
}