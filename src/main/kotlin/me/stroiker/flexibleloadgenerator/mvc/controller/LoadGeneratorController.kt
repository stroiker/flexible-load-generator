package me.stroiker.flexibleloadgenerator.mvc.controller

import me.stroiker.flexibleloadgenerator.LoadGenerator
import me.stroiker.flexibleloadgenerator.Response
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorProgressResponse
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorStartRequest
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorStatusResponse
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorStopResponse
import me.stroiker.flexibleloadgenerator.success
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/flexible-load-generator")
internal class LoadGeneratorController(private val generator: LoadGenerator) {

    @PostMapping("/start")
    fun start(@RequestBody request: LoadGeneratorStartRequest): Response<LoadGeneratorStatusResponse> =
        success(generator.start(request))

    @PutMapping("/stop")
    fun stop(): Response<LoadGeneratorStopResponse> = success(generator.stop())

    @GetMapping("/status")
    fun getStatus(): Response<LoadGeneratorStatusResponse> = success(generator.getStatus())

    @GetMapping("/progress")
    fun getProgress(): Flux<ServerSentEvent<LoadGeneratorProgressResponse>> = generator.getProgress()
}
