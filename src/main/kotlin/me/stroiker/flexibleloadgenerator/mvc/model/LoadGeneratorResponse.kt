package me.stroiker.flexibleloadgenerator.mvc.model

import com.fasterxml.jackson.annotation.JsonProperty

internal open class LoadGeneratorResponse(
    @get:JsonProperty("isRunning")
    val isRunning: Boolean
)