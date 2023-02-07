package me.stroiker.flexibleloadgenerator.mvc.model

import com.fasterxml.jackson.annotation.JsonProperty

internal open class LoadGeneratorStatusResponse(
    @get:JsonProperty("isRunning")
    val isRunning: Boolean,
    val taskId: String?
)