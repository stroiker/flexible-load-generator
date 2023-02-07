package me.stroiker.flexibleloadgenerator.mvc.model

internal class LoadGeneratorStartRequest(
    val segments: Map<Long, Long> = mapOf(),
    val timeScale: Long? = null,
)
