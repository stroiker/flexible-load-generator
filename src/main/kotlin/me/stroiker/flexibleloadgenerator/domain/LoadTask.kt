package me.stroiker.flexibleloadgenerator.domain

import java.util.*

class LoadTask(private val loadProfile: LoadProfile) {

    val taskId = UUID.randomUUID().toString()

    val phases = loadProfile.segments.map { ops ->
        LoadPhase(loadProfile.timeScale, ops)
    }
}