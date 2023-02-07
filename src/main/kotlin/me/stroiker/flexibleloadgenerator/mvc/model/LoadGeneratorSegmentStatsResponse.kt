package me.stroiker.flexibleloadgenerator.mvc.model

internal class LoadGeneratorSegmentStatsResponse (
    val segmentNumber: Int,
    val estimatedOPS: Long,
    val actualOPS: Long,
    val processedOperations: Long
): LoadGeneratorProgressResponse(false)