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

package me.stroiker.easyloadgenerator

import me.stroiker.easyloadgenerator.LoadType.*
import me.stroiker.easyloadgenerator.mvc.model.LoadProfileResponse
import java.io.File
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter
import java.util.concurrent.Future
import kotlin.math.asinh

open class LoadProfile private constructor() {

    private var cycles = 0

    private var steps: List<LoadStep>? = null
    var result: List<Pair<LocalDateTime?, Long>>? = null
        private set

    fun run() {
        cycles = steps?.map { it.phase.duration.seconds }?.sum()?.toInt() ?: 0
        result = ArrayList(cycles)
        steps?.firstOrNull()?.also { it.run(result!!) }
    }

    fun getLoadProfileInfo(height: Int, width: Int) = consoleGraph(getLoadProfileData().map { it.second }, height, width)

    fun getCsvResult() = result?.let { csvFile(it) }

    fun getProgressPercentage() = result?.let { it.size * 100 / cycles } ?: 0

    private fun getLoadProfileData() = mutableListOf<Pair<LocalDateTime?, Long>>().apply {
        val tmp = mutableListOf<LoadStep>()
        steps?.reversed()?.forEach { step ->
            tmp.add(
                LoadStep(
                    phase = LoadPhase(
                        type = step.phase.type,
                        duration = step.phase.duration,
                        scaleFactor = step.phase.scaleFactor,
                        stretchFactor = step.phase.stretchFactor
                    ), next = if (tmp.isNotEmpty()) tmp.last() else null, job = null, callback = {}
                )
            )
        }
        tmp.reversed().firstOrNull()?.run(this)
    }

    private fun getGraph(values: Collection<Long>, height: Int, width: Int): LoadProfileResponse = consoleGraph(values, height, width)

    class Builder {

        private val phases = mutableListOf<LoadPhase>()
        private var job: ((times: Long) -> Future<*>)? = null
        private var callback: (() -> Unit)? = null

        fun job(job: ((times: Long) -> Future<*>)): Builder {
            this.job = job
            return this
        }

        fun phase(phase: LoadPhase): Builder {
            phases.add(phase)
            return this
        }

        fun onEndCallback(callback: () -> Unit): Builder {
            this.callback = callback
            return this
        }

        fun build(): LoadProfile =
            job?.let {
                val tmp = mutableListOf<LoadStep>()
                phases.reversed().forEach { phase ->
                    tmp.add(
                        LoadStep(
                            phase = phase,
                            next = if (tmp.isNotEmpty()) tmp.last() else null,
                            job = job,
                            callback = callback ?: throw IllegalStateException("Callback must be configured")
                        )
                    )
                }
                LoadProfile().apply { steps = tmp.reversed() }
            } ?: throw IllegalStateException("Job must be configured")
    }

    class LoadStep(
        val phase: LoadPhase,
        val next: LoadStep? = null,
        private val job: ((times: Long) -> Future<*>)?,
        private val callback: () -> Unit
    ) {
        fun run(result: List<Pair<LocalDateTime?, Long>>) {
            result as MutableList<Pair<LocalDateTime?, Long>>
            var jobResult: Future<*>? = null
            when (phase.type) {
                UP -> {
                    val halfCycleCount = phase.duration.seconds / 2
                    var idx = -halfCycleCount
                    val alignOffset =
                        result.lastOrNull()?.second?.minus(((asinh(idx.toDouble() / (halfCycleCount * phase.stretchFactor!!)) * (halfCycleCount * phase.stretchFactor)) + halfCycleCount) * phase.scaleFactor!!)
                            ?: 0.0
                    do {
                        ((((asinh(idx++.toDouble() / (halfCycleCount * phase.stretchFactor!!)) * (halfCycleCount * phase.stretchFactor)) + halfCycleCount) * phase.scaleFactor!! + alignOffset).takeIf { it >= 0.0 }
                            ?: 0.0).toLong().also { value ->
                            result.add(Pair(job?.let { now() }, value))
                            job?.also {
                                jobResult = invokeJob(value, it)
                            }
                        }
                    } while (idx < halfCycleCount)
                }
                DOWN -> {
                    val halfCycleCount = phase.duration.seconds / 2
                    var idx = halfCycleCount
                    val alignOffset =
                        result.lastOrNull()?.second?.minus(((asinh(idx.toDouble() / (halfCycleCount * phase.stretchFactor!!)) * (halfCycleCount * phase.stretchFactor)) + halfCycleCount) * phase.scaleFactor!!)
                            ?: 0.0
                    do {
                        ((((asinh(idx--.toDouble() / (halfCycleCount * phase.stretchFactor!!)) * (halfCycleCount * phase.stretchFactor)) + halfCycleCount) * phase.scaleFactor!! + alignOffset).takeIf { it >= 0.0 }
                            ?: 0.0).toLong().also { value ->
                            result.add(Pair(job?.let { now() }, value))
                            job?.also {
                                jobResult = invokeJob(value, it)
                            }
                        }
                    } while (idx > -halfCycleCount)
                }
                FLAT -> {
                    val cycleCount = phase.duration.seconds
                    var idx = 0L
                    do {
                        (result.lastOrNull()?.second?.takeIf { it >= 0 } ?: 0).also { value ->
                            result.add(Pair(job?.let { now() }, value))
                            job?.also {
                                jobResult = invokeJob(value, it)
                            }
                        }
                        idx++
                    } while (idx < cycleCount)
                }
            }
            next?.run(result) ?: run { jobResult?.get().also { callback() } }
        }

        private inline fun invokeJob(value: Long, job: ((times: Long) -> Future<*>)): Future<*> = job(value)
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun builder(): Builder = Builder()

        fun consoleGraph(data: Collection<Long>, height: Int, width: Int): LoadProfileResponse {
            val array: Array<Array<String>> = Array(height) { Array(width) { "_" } }

            val yScale = data.maxOrNull()?.toLong()?.plus(height - 1)?.div(height) ?: 1
            val xScale = data.size.toDouble().plus(width - 1).div(width).toInt()
            var summ = 0.0
            var yIdx = 0
            data.forEachIndexed { index, value ->
                if (index % xScale == 0) {
                    summ += value
                    val avg = summ.div(yScale)
                    val idx = ((height - (avg.div(xScale))).toInt() - 1).takeIf { it >= 0 } ?: 0
                    array[idx][yIdx] = "*"
                    yIdx++
                    summ = 0.0
                } else {
                    summ += value
                }
            }
            val overall = data.sum()
            return StringBuilder().apply {
                array.forEach { x ->
                    x.forEach { y ->
                        append(y)
                    }
                    append("\n")
                }
            }.toString().let { graph ->
                LoadProfileResponse(
                    overall = overall,
                    max = data.maxOrNull(),
                    min = data.minOrNull(),
                    average = overall / data.size,
                    duration = data.size,
                    graph = graph
                )
            }
        }

        fun csvFile(data: List<Pair<LocalDateTime?, Long>>): File {
            data as MutableList<Pair<LocalDateTime?, Long>>
            val file = File.createTempFile("csv", ".csv")
            PrintWriter(file).apply {
                write("\"\";\"Time\";\"OPS\"\n")
                data.forEach { pair ->
                    write("\"\";\"${pair.first?.let { formatter.format(it) } ?: 0L}\";\"${pair.second}\"\n")
                }
                flush()
            }
            return file
        }
    }
}
