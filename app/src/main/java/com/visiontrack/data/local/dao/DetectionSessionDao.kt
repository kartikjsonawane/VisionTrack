package com.visiontrack.data.local.dao

import androidx.room.*
import com.visiontrack.data.local.entity.DetectionSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(entity: DetectionSessionEntity)

    @Update
    suspend fun updateSession(entity: DetectionSessionEntity)

    @Query("SELECT * FROM detection_sessions WHERE userId = :userId ORDER BY startTimeMs DESC")
    fun getSessionsForUser(userId: String): Flow<List<DetectionSessionEntity>>

    @Query("SELECT * FROM detection_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): DetectionSessionEntity?

    @Query("SELECT * FROM detection_sessions WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedSessions(userId: String): List<DetectionSessionEntity>

    @Query("UPDATE detection_sessions SET isSynced = 1 WHERE id = :sessionId")
    suspend fun markSynced(sessionId: String)

    @Query("UPDATE detection_sessions SET endTimeMs = :endMs, totalDetections = :count, avgFps = :fps, avgLatencyMs = :latency, uniqueLabels = :labels WHERE id = :sessionId")
    suspend fun closeSession(
        sessionId: String,
        endMs: Long,
        count: Int,
        fps: Float,
        latency: Float,
        labels: String
    )

    @Delete
    suspend fun deleteSession(entity: DetectionSessionEntity)

    @Query("DELETE FROM detection_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)
}
