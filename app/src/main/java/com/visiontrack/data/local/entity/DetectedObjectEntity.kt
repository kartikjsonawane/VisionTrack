package com.visiontrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detected_objects",
    foreignKeys = [
        ForeignKey(
            entity = DetectionSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("label"), Index("timestampMs")]
)
data class DetectedObjectEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val userId: String,
    val label: String,
    val confidence: Float,
    val boundingBoxLeft: Float,
    val boundingBoxTop: Float,
    val boundingBoxRight: Float,
    val boundingBoxBottom: Float,
    val trackingId: Int?,
    val timestampMs: Long,
    val zoneId: String?
)

@Entity(tableName = "detection_sessions", indices = [Index("userId"), Index("startTimeMs")])
data class DetectionSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val totalDetections: Int,
    val uniqueLabels: String,   // JSON array
    val avgFps: Float,
    val avgLatencyMs: Float,
    val thumbnailUrl: String?,
    val notes: String,
    val isSynced: Boolean
)
