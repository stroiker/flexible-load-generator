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

import me.stroiker.easyloadgenerator.LoadGenerator
import me.stroiker.easyloadgenerator.LoadGeneratorJob
import me.stroiker.easyloadgenerator.mvc.LoadGeneratorWebExceptionHandler
import me.stroiker.easyloadgenerator.mvc.controller.LoadGeneratorController
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Conditional(LoadGeneratorAutoConfigurationCondition::class)
@EnableConfigurationProperties(LoadGeneratorProperties::class)
@Import(LoadGeneratorController::class, LoadGeneratorWebExceptionHandler::class)
open class LoadGeneratorConfig {

    @Bean
    open fun loadGenerator(properties: LoadGeneratorProperties, job: LoadGeneratorJob) = LoadGenerator(properties, job)
}
