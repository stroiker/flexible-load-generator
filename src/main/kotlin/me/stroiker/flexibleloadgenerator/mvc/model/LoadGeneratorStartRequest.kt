package me.stroiker.flexibleloadgenerator.mvc.model

internal class LoadGeneratorStartRequest(
    val segments: Map<Int, Int> = mapOf(),
    val timeScale: Int? = null,
)
