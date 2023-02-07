package me.stroiker.flexibleloadgenerator

import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorBasicResponse
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorResponseStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal typealias Response<T> = ResponseEntity<LoadGeneratorBasicResponse<T>>

internal fun <T> success(result: T): Response<T> =
    ResponseEntity(LoadGeneratorBasicResponse(result, LoadGeneratorResponseStatus.SUCCESS), HttpStatus.OK)

internal fun error(message: String?): Response<Unit> =
    ResponseEntity(LoadGeneratorBasicResponse(responseStatus = LoadGeneratorResponseStatus.ERROR, errorMessage = message), HttpStatus.BAD_REQUEST)