package me.stroiker.flexibleloadgenerator.mvc.model

internal class LoadGeneratorStopResponse(
    isRunning: Boolean,
    taskId: String,
): LoadGeneratorStatusResponse(isRunning, taskId)