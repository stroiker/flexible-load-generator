package me.stroiker.flexibleloadgenerator.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "load-generator")
internal class LoadGeneratorProperties {
    var threadPoolSize: Int? = null
}