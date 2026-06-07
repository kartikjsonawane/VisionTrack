package com.visiontrack.domain.model

import android.graphics.RectF

/**
 * Core domain model representing a single detected object from the ML inference pipeline.
 *
 * @param id           Unique identifier for this detection event
 * @param label        Human-readable class name (e.g., "person", "car")
 * @param confidence   Model confidence [0.0, 1.0]
 * @param boundingBox  Normalized bounding box in image coordinate space
 * @param trackingId   Optional multi-frame tracking ID (assigned by tracker)
 * @param timestampMs  Unix timestamp in milliseconds when detected
 * @param sessionId    ID of the detection session this belongs to
 * @param zoneId       Optional zone ID if zone-based detection is active
 */
data class DetectedObject(
    val id: String,
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val trackingId: Int? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val sessionId: String = "",
    val zoneId: String? = null
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()
    val displayLabel: String get() = "$label ${confidencePercent}%"
}

/** Aggregated analytics per class label. */
data class ObjectFrequency(
    val label: String,
    val count: Int,
    val avgConfidence: Float,
    val lastSeen: Long
)

/** Zone definition for zone-based detection alerting. */
data class DetectionZone(
    val id: String,
    val name: String,
    val rect: RectF,          // normalized [0,1] coordinates
    val color: Long = 0xFF4CAF50,
    val alertOnEntry: Boolean = true,
    val alertOnExit: Boolean = false,
    val targetLabels: List<String> = emptyList() // empty = all labels
)

/** Alert triggered when an object enters / exits a zone or exceeds a threshold. */
data class DetectionAlert(
    val id: String,
    val type: AlertType,
    val message: String,
    val detectedObject: DetectedObject,
    val zone: DetectionZone? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class AlertType { ZONE_ENTRY, ZONE_EXIT, COUNT_THRESHOLD, HIGH_CONFIDENCE }
