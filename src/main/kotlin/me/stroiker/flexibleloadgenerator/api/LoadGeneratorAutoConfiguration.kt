package me.stroiker.flexibleloadgenerator.api

import org.springframework.stereotype.Component

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class LoadGeneratorAutoConfiguration
