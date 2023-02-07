package me.stroiker.flexibleloadgenerator.api

interface LoadGeneratorJob {
    fun onEach()
    fun onStart() {}
    fun onFinish() {}
}
