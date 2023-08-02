package me.stroiker.flexibleloadgenerator.domain

import java.util.concurrent.atomic.AtomicInteger

class LoadTaskSegmentSummary {
    val startTimestamp = System.currentTimeMillis()
    val successJobCount = AtomicInteger()
    val failedJobCount = AtomicInteger()
    val successLatencyList = mutableListOf<Long>()
    val failedLatencyList = mutableListOf<Long>()
}