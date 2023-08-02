package me.stroiker.flexibleloadgenerator.config

import me.stroiker.flexibleloadgenerator.generator.LoadGenerator
import me.stroiker.flexibleloadgenerator.api.LoadGeneratorJob
import me.stroiker.flexibleloadgenerator.mvc.controller.LoadGeneratorController
import me.stroiker.flexibleloadgenerator.mvc.handler.LoadGeneratorHandler
import me.stroiker.flexibleloadgenerator.mvc.handler.LoadGeneratorWebExceptionHandler
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Conditional(LoadGeneratorAutoConfigurationCondition::class)
@EnableConfigurationProperties(LoadGeneratorProperties::class)
@Import(LoadGeneratorController::class, LoadGeneratorWebExceptionHandler::class)
internal open class LoadGeneratorConfig {

    @Bean
    open fun loadGeneratorHandler(properties: LoadGeneratorProperties, job: LoadGeneratorJob) =
        LoadGeneratorHandler(LoadGenerator(properties, job))
}
