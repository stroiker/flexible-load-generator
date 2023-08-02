package me.stroiker.flexibleloadgenerator.utils

import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorBasicResponse
import me.stroiker.flexibleloadgenerator.mvc.model.LoadGeneratorResponseStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import kotlin.math.roundToInt

internal typealias Response<T> = ResponseEntity<LoadGeneratorBasicResponse<T>>

internal fun <T> success(result: T): Response<T> =
    ResponseEntity(LoadGeneratorBasicResponse(result, LoadGeneratorResponseStatus.SUCCESS), HttpStatus.OK)

internal fun error(message: String?): Response<Unit> =
    ResponseEntity(
        LoadGeneratorBasicResponse(
            responseStatus = LoadGeneratorResponseStatus.ERROR,
            errorMessage = message
        ), HttpStatus.BAD_REQUEST
    )

fun calculateTm90(source: List<Long>): Long =
    if (source.isEmpty())
        0
    else
        source.sorted().let { sortedSource ->
            val lastValueIdx = (sortedSource.size * 0.9).roundToInt()
            if(lastValueIdx > 0)
            sortedSource.subList(0, lastValueIdx).let { trimmedSource ->
                trimmedSource.sum() / trimmedSource.size
            }
            else
                sortedSource.sum() /sortedSource.size
        }