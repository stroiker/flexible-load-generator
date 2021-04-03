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

package me.stroiker.easyloadgenerator.mvc.controller

import me.stroiker.easyloadgenerator.LoadGenerator
import me.stroiker.easyloadgenerator.LoadGeneratorState
import me.stroiker.easyloadgenerator.LoadPhase
import me.stroiker.easyloadgenerator.LoadType
import me.stroiker.easyloadgenerator.mvc.model.LoadGeneratorProfileRequest
import me.stroiker.easyloadgenerator.mvc.model.LoadGeneratorStatusResponse
import me.stroiker.easyloadgenerator.mvc.model.LoadProfileResponse
import org.apache.commons.io.IOUtils
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.Duration
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/load-generator")
class LoadGeneratorController(private val generator: LoadGenerator) {

    @GetMapping("/index.html")
    fun getIndexPage(response: HttpServletResponse) {
        IOUtils.copy(javaClass.classLoader.getResourceAsStream("index.html"), response.outputStream)
    }

    @PutMapping("/start")
    fun start() = generator.start()

    @PutMapping("/stop")
    fun stop() = generator.stop()

    @PostMapping("/profile")
    fun createLoadProfile(@RequestBody request: LoadGeneratorProfileRequest) {
        if (getStatus().status == LoadGeneratorState.ON)
            throw IllegalStateException("Creating load profile is forbidden when load generator is running")
        request.phases.map {
            LoadPhase(
                type = LoadType.valueOf(it.type!!),
                duration = Duration.ofSeconds(it.duration!!),
                stretchFactor = it.stretchFactor,
                scaleFactor = it.scaleFactor
            )
        }.also { generator.createLoadProfile(it) }
    }

    @GetMapping("/profile")
    fun getProfile(): LoadProfileResponse? = generator.getLoadProfile()

    @GetMapping("/status")
    fun getStatus(): LoadGeneratorStatusResponse = generator.getStatus()

    @GetMapping("/progress")
    fun getProgress(): Flux<ServerSentEvent<Int>> = generator.getProgress()

    @GetMapping("/result.csv", produces = ["text/csv"])
    fun getResultCsv(response: HttpServletResponse) {
        response.contentType = "text/csv"
        generator.profile?.getCsvResult()?.also { IOUtils.copy(it.inputStream(), response.outputStream) }
    }
}
