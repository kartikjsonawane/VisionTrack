package com.visiontrack.data.local.dao

import androidx.room.*
import com.visiontrack.data.local.entity.DetectedObjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetection(entity: DetectedObjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetections(entities: List<DetectedObjectEntity>)

    @Query("SELECT * FROM detected_objects WHERE sessionId = :sessionId ORDER BY timestampMs DESC")
    fun getDetectionsForSession(sessionId: String): Flow<List<DetectedObjectEntity>>

    @Query("SELECT * FROM detected_objects WHERE userId = :userId ORDER BY timestampMs DESC")
    fun getAllDetectionsForUser(userId: String): Flow<List<DetectedObjectEntity>>

    @Query("SELECT COUNT(*) FROM detected_objects WHERE userId = :userId")
    suspend fun getDetectionCount(userId: String): Int

    @Query("""
        SELECT label, COUNT(*) as count, AVG(confidence) as avgConfidence, MAX(timestampMs) as lastSeen
        FROM detected_objects
        WHERE userId = :userId
        GROUP BY label
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getObjectFrequencies(userId: String, limit: Int): List<LabelFrequency>

    @Query("""
        SELECT strftime('%H', timestampMs / 1000, 'unixepoch') as hour, COUNT(*) as count
        FROM detected_objects
        WHERE userId = :userId AND timestampMs >= :sinceMs
        GROUP BY hour
    """)
    suspend fun getDetectionCountByHour(userId: String, sinceMs: Long): List<HourlyCount>

    @Query("""
        SELECT date(timestampMs / 1000, 'unixepoch') as day, COUNT(*) as count
        FROM detected_objects
        WHERE userId = :userId AND timestampMs >= :sinceMs
        GROUP BY day
        ORDER BY day DESC
    """)
    suspend fun getDetectionCountByDay(userId: String, sinceMs: Long): List<DailyCount>

    @Query("DELETE FROM detected_objects WHERE sessionId = :sessionId")
    suspend fun deleteDetectionsForSession(sessionId: String)

    @Query("SELECT * FROM detected_objects WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    suspend fun getDetectionsForExport(sessionId: String): List<DetectedObjectEntity>
}

data class LabelFrequency(val label: String, val count: Int, val avgConfidence: Float, val lastSeen: Long)
data class HourlyCount(val hour: String, val count: Int)
data class DailyCount(val day: String, val count: Int)
