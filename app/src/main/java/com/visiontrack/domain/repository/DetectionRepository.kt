package com.visiontrack.domain.repository

import com.visiontrack.domain.model.DetectedObject
import com.visiontrack.domain.model.DetectionSession
import com.visiontrack.domain.model.ObjectFrequency
import kotlinx.coroutines.flow.Flow

interface DetectionRepository {

    // Sessions
    suspend fun startSession(userId: String): DetectionSession
    suspend fun endSession(sessionId: String, avgFps: Float = 0f, avgLatencyMs: Float = 0f): DetectionSession
    fun getSessionsForUser(userId: String): Flow<List<DetectionSession>>
    suspend fun getSessionById(sessionId: String): DetectionSession?
    suspend fun deleteSession(sessionId: String)

    // Detections
    suspend fun saveDetection(obj: DetectedObject)
    suspend fun saveDetections(objects: List<DetectedObject>)
    fun getDetectionsForSession(sessionId: String): Flow<List<DetectedObject>>
    fun getAllDetectionsForUser(userId: String): Flow<List<DetectedObject>>
    suspend fun getDetectionCount(userId: String): Int
    suspend fun getObjectFrequencies(userId: String, limit: Int = 10): List<ObjectFrequency>

    // Export
    suspend fun exportSessionToCsv(sessionId: String): String  // returns file path
    suspend fun syncSessionToCloud(sessionId: String): Result<Unit>

    // Analytics
    suspend fun getDetectionCountByHour(userId: String, days: Int = 7): Map<Int, Int>
    suspend fun getTotalDetectionsByDay(userId: String, days: Int = 30): Map<String, Int>
}
