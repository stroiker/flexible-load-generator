package me.stroiker.flexibleloadgenerator.mvc.model

internal class LoadGeneratorBasicResponse<T>(
    val result: T? = null,
    val responseStatus: LoadGeneratorResponseStatus,
    val errorMessage: String? = null
)