package me.stroiker.flexibleloadgenerator.mvc.handler

import me.stroiker.flexibleloadgenerator.Response
import me.stroiker.flexibleloadgenerator.error
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
internal class LoadGeneratorWebExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(Throwable::class)
    fun handle(throwable: Throwable): Response<Unit> = error(throwable.message)
}