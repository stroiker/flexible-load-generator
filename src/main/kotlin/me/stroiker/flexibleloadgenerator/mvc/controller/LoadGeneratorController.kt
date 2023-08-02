package me.stroiker.flexibleloadgenerator.mvc.controller

import kotlinx.coroutines.DelicateCoroutinesApi
import me.stroiker.flexibleloadgenerator.mvc.handler.LoadGeneratorHandler
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorProgressResponse
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorResponse
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorStartRequest
import me.stroiker.flexibleloadgenerator.utils.Response
import me.stroiker.flexibleloadgenerator.utils.success
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/flexible-load-generator")
internal class LoadGeneratorController(private val handler: LoadGeneratorHandler) {

    @PostMapping("/start")
    fun start(@RequestBody request: LoadGeneratorStartRequest): Response<LoadGeneratorResponse> =
        success(handler.start(request))

    @DelicateCoroutinesApi
    @PutMapping("/stop")
    fun stop(): Response<LoadGeneratorResponse> = success(handler.stop())

    @GetMapping("/status")
    fun status(): Response<LoadGeneratorResponse> = success(handler.status())

    @GetMapping("/progress")
    fun progress(): Flux<ServerSentEvent<LoadGeneratorProgressResponse>> = handler.progress()
}
