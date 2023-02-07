package me.stroiker.flexibleloadgenerator.config

import me.stroiker.flexibleloadgenerator.api.LoadGeneratorAutoConfiguration
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

internal class LoadGeneratorAutoConfigurationCondition: Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) =
        context.beanFactory?.getBeanNamesForAnnotation(LoadGeneratorAutoConfiguration::class.java)?.isNotEmpty()
            ?: false
}