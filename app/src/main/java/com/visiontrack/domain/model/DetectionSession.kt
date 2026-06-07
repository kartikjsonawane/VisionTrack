package com.visiontrack.domain.model

/**
 * A detection session represents one run of the live detector.
 * Groups all [DetectedObject]s captured between start and stop.
 */
data class DetectionSession(
    val id: String,
    val userId: String,
    val startTimeMs: Long,
    val endTimeMs: Long? = null,
    val totalDetections: Int = 0,
    val uniqueLabels: List<String> = emptyList(),
    val avgFps: Float = 0f,
    val avgLatencyMs: Float = 0f,
    val thumbnailUrl: String? = null,
    val notes: String = "",
    val isSynced: Boolean = false
) {
    val durationMs: Long get() = (endTimeMs ?: System.currentTimeMillis()) - startTimeMs
    val isActive: Boolean get() = endTimeMs == null
}

/** Performance snapshot for a single inference frame. */
data class InferenceMetrics(
    val frameIndex: Long,
    val preProcessMs: Long,
    val inferenceMs: Long,
    val postProcessMs: Long,
    val objectCount: Int
) {
    val totalMs: Long get() = preProcessMs + inferenceMs + postProcessMs
    val fps: Float get() = if (totalMs > 0) 1000f / totalMs else 0f
}
