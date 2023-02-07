package me.stroiker.flexibleloadgenerator.domain

data class LoadProfile(
    val segments: Collection<Long>,
    val timeScale: Long
)
