package com.visiontrack.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.visiontrack.domain.model.DetectedObject
import com.visiontrack.domain.model.DetectionAlert
import com.visiontrack.domain.model.DetectionZone
import com.visiontrack.domain.model.AlertType
import com.visiontrack.domain.model.InferenceMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level wrapper around [YOLOv8Detector].
 *
 * Responsibilities:
 *  - Convert [Detection] → [DetectedObject] domain models
 *  - Multi-frame object tracking (centroid-based IoU tracker)
 *  - Zone-based detection and alert emission
 *  - Rolling FPS and latency statistics
 *  - Screenshot-frame capture
 */
@Singleton
class ObjectDetectionHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detector: YOLOv8Detector
) {
    // Active detection zones (configured by user in Settings)
    val zones: MutableList<DetectionZone> = mutableListOf()

    // Alert stream
    private val _alerts = MutableSharedFlow<DetectionAlert>(extraBufferCapacity = 64)
    val alerts: SharedFlow<DetectionAlert> = _alerts

    // Rolling metrics (last 30 frames)
    private val metricsBuffer = ArrayDeque<InferenceMetrics>(30)

    // Tracker state: trackingId → last known bounding box
    private val trackerState = mutableMapOf<Int, RectF>()
    private var nextTrackId = 1

    val averageFps: Float
        get() = if (metricsBuffer.isEmpty()) 0f else
            metricsBuffer.map { it.fps }.average().toFloat()

    val averageLatencyMs: Float
        get() = if (metricsBuffer.isEmpty()) 0f else
            metricsBuffer.map { it.totalMs }.average().toFloat()

    /**
     * Process a single camera frame.
     *
     * @param bitmap         The camera frame (RGBA or RGB)
     * @param sessionId      Active session ID to stamp on results
     * @param frameIndex     Monotonic frame counter
     * @return               Detected and tracked domain objects
     */
    fun processFrame(bitmap: Bitmap, sessionId: String, frameIndex: Long): List<DetectedObject> {
        val result = detector.detect(bitmap)

        // Track metrics
        val metrics = InferenceMetrics(
            frameIndex    = frameIndex,
            preProcessMs  = result.preProcessMs,
            inferenceMs   = result.inferenceMs,
            postProcessMs = result.postProcessMs,
            objectCount   = result.detections.size
        )
        if (metricsBuffer.size >= 30) metricsBuffer.removeFirst()
        metricsBuffer.addLast(metrics)

        // Convert → domain + assign tracking IDs
        val domainObjects = result.detections.map { det ->
            val trackId = assignTrackingId(det.boundingBox)
            DetectedObject(
                id          = UUID.randomUUID().toString(),
                label       = det.label,
                confidence  = det.confidence,
                boundingBox = det.boundingBox,
                trackingId  = trackId,
                sessionId   = sessionId
            )
        }

        // Zone-based alert check
        if (zones.isNotEmpty()) {
            checkZones(domainObjects, bitmap.width, bitmap.height)
        }

        return domainObjects
    }

    /** Greedy centroid-IoU tracker. Assigns existing IDs or mints new ones. */
    private fun assignTrackingId(box: RectF): Int {
        var bestId   = -1
        var bestIou  = 0.5f   // minimum IoU to claim an existing track

        trackerState.forEach { (id, prevBox) ->
            val iou = computeIoU(box, prevBox)
            if (iou > bestIou) {
                bestIou = iou
                bestId  = id
            }
        }

        val trackId = if (bestId >= 0) bestId else nextTrackId++
        trackerState[trackId] = box

        // Evict stale tracks not updated this frame
        if (trackerState.size > 100) {
            val toRemove = trackerState.keys.filter { it != trackId }.take(20)
            toRemove.forEach { trackerState.remove(it) }
        }

        return trackId
    }

    private fun computeIoU(a: RectF, b: RectF): Float {
        val ix = maxOf(0f, minOf(a.right, b.right) - maxOf(a.left, b.left))
        val iy = maxOf(0f, minOf(a.bottom, b.bottom) - maxOf(a.top, b.top))
        val inter = ix * iy
        val union = a.width() * a.height() + b.width() * b.height() - inter
        return if (union <= 0f) 0f else inter / union
    }

    private fun checkZones(objects: List<DetectedObject>, imgW: Int, imgH: Int) {
        zones.forEach { zone ->
            // Convert zone from normalized to pixel
            val zonePixel = RectF(
                zone.rect.left  * imgW,
                zone.rect.top   * imgH,
                zone.rect.right * imgW,
                zone.rect.bottom* imgH
            )
            objects.forEach { obj ->
                if (zone.targetLabels.isEmpty() || obj.label in zone.targetLabels) {
                    if (RectF.intersects(zonePixel, obj.boundingBox)) {
                        val alert = DetectionAlert(
                            id             = UUID.randomUUID().toString(),
                            type           = AlertType.ZONE_ENTRY,
                            message        = "${obj.label} detected in zone '${zone.name}'",
                            detectedObject = obj,
                            zone           = zone
                        )
                        _alerts.tryEmit(alert)
                    }
                }
            }
        }
    }

    fun resetTracker() { trackerState.clear(); nextTrackId = 1 }

    fun close() { detector.close() }
}
