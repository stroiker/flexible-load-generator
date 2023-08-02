package me.stroiker.flexibleloadgenerator.mvc.model

internal class LoadGeneratorSegmentStatsResponse (
    taskId: String,
    val segmentNumber: Int,
    val successOperationsCount: Int,
    val failedOperationsCount: Int,
    val expectedOps: Int,
    val actualOps: Int,
    val tm90SuccessLatencyNano: Long,
    val tm90FailedLatencyNano: Long
): LoadGeneratorProgressResponse(taskId, false)