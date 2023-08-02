package me.stroiker.flexibleloadgenerator.domain

data class LoadProfile(
    val segments: Collection<Int>,
    val timeScale: Int
)
