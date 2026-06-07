package com.visiontrack.data.mapper

import android.graphics.RectF
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.visiontrack.data.local.dao.LabelFrequency
import com.visiontrack.data.local.entity.DetectedObjectEntity
import com.visiontrack.data.local.entity.DetectionSessionEntity
import com.visiontrack.domain.model.DetectedObject
import com.visiontrack.domain.model.DetectionSession
import com.visiontrack.domain.model.ObjectFrequency

private val gson = Gson()

fun DetectedObjectEntity.toDomain() = DetectedObject(
    id = id,
    label = label,
    confidence = confidence,
    boundingBox = RectF(boundingBoxLeft, boundingBoxTop, boundingBoxRight, boundingBoxBottom),
    trackingId = trackingId,
    timestampMs = timestampMs,
    sessionId = sessionId,
    zoneId = zoneId
)

fun DetectedObject.toEntity(userId: String) = DetectedObjectEntity(
    id = id,
    sessionId = sessionId,
    userId = userId,
    label = label,
    confidence = confidence,
    boundingBoxLeft = boundingBox.left,
    boundingBoxTop = boundingBox.top,
    boundingBoxRight = boundingBox.right,
    boundingBoxBottom = boundingBox.bottom,
    trackingId = trackingId,
    timestampMs = timestampMs,
    zoneId = zoneId
)

fun DetectionSessionEntity.toDomain(): DetectionSession {
    val labels: List<String> = try {
        gson.fromJson(uniqueLabels, object : TypeToken<List<String>>() {}.type) ?: emptyList()
    } catch (e: Exception) { emptyList() }

    return DetectionSession(
        id = id,
        userId = userId,
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        totalDetections = totalDetections,
        uniqueLabels = labels,
        avgFps = avgFps,
        avgLatencyMs = avgLatencyMs,
        thumbnailUrl = thumbnailUrl,
        notes = notes,
        isSynced = isSynced
    )
}

fun DetectionSession.toEntity() = DetectionSessionEntity(
    id = id,
    userId = userId,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    totalDetections = totalDetections,
    uniqueLabels = gson.toJson(uniqueLabels),
    avgFps = avgFps,
    avgLatencyMs = avgLatencyMs,
    thumbnailUrl = thumbnailUrl,
    notes = notes,
    isSynced = isSynced
)

fun LabelFrequency.toDomain() = ObjectFrequency(
    label = label,
    count = count,
    avgConfidence = avgConfidence,
    lastSeen = lastSeen
)
