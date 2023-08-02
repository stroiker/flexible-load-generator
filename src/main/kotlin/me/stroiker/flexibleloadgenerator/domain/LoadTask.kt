package me.stroiker.flexibleloadgenerator.domain

import java.util.*

class LoadTask(private val loadProfile: LoadProfile) {

    val taskId = UUID.randomUUID().toString()

    val segments = loadProfile.segments.map { ops ->
        LoadSegment(loadProfile.timeScale, ops)
    }
}