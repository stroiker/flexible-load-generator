/*
 *
 *  Copyright 2021 @stroiker
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package me.stroiker.easyloadgenerator.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket

@Configuration
@Conditional(LoadGeneratorAutoConfigurationCondition::class)
@PropertySource("classpath:version.properties")
open class SwaggerConfig {

    @Value("\${version}")
    private lateinit var version: String

    @Bean
    @ConditionalOnMissingBean
    open fun swaggerUiApi() = Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.basePackage("me.stroiker.easyloadgenerator.mvc.controller"))
        .paths(PathSelectors.ant("/load-generator/*"))
        .build()
        .apiInfo(
            ApiInfoBuilder()
                .title("Easy Load Generator")
                .version(version)
                .description("Library for generating customizable and flexible load")
                .contact(Contact("Eugene Ermakov", "github.com/stroiker", "stroiker2014@gmail.com"))
                .license("Apache 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
                .build()
        )
}