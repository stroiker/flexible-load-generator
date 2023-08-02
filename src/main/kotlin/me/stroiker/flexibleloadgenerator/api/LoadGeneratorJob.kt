package me.stroiker.flexibleloadgenerator.api

interface LoadGeneratorJob {
    fun onEach(): Boolean
    fun onStart() {}
    fun onFinish() {}
}
