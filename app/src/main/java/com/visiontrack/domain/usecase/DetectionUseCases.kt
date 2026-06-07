package com.visiontrack.domain.usecase

import com.visiontrack.domain.model.DetectedObject
import com.visiontrack.domain.model.DetectionSession
import com.visiontrack.domain.model.ObjectFrequency
import com.visiontrack.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Bundles all detection-related use cases for clean ViewModel injection. */
data class DetectionUseCases(
    val startSession: StartDetectionSessionUseCase,
    val endSession: EndDetectionSessionUseCase,
    val saveDetections: SaveDetectionsUseCase,
    val getSessionHistory: GetSessionHistoryUseCase,
    val getObjectFrequencies: GetObjectFrequenciesUseCase,
    val exportSessionCsv: ExportSessionCsvUseCase,
    val syncSession: SyncSessionUseCase,
    val getAnalytics: GetDetectionAnalyticsUseCase
)

class StartDetectionSessionUseCase @Inject constructor(
    private val repo: DetectionRepository
) {
    suspend operator fun invoke(userId: String): DetectionSession = repo.startSession(userId)
}

class EndDetectionSessionUseCase @Inject constructor(
    private val repo: DetectionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        avgFps: Float = 0f,
        avgLatencyMs: Float = 0f
    ): DetectionSession = repo.endSession(sessionId, avgFps, avgLatencyMs)
}

class SaveDetectionsUseCase @Inject constructor(
    private val repo: DetectionRepository
) {
    suspend operator fun invoke(objects: List<DetectedObject>) {
        if (objects.isNotEmpty()) repo.saveDetections(objects)
    }
}

class GetSessionHistoryUseCase @Inject constructor(
    private val repo: DetectionRepository
) {
    operator fun invoke(userId: String): Flow<List<DetectionSession>> =
        repo.getSessionsForUser(userId)
}

class GetObjectFrequenciesUseCase @Inject constructor(
    private val repo: DetectionRepository
) {
    suspend operator fun invoke(userId: String, limit: Int = 10): List<ObjectFrequency> =
        repo.getObjectFrequencies(userId, limit)
}

class ExportSessionCsvUseCase @Inject constructor(
    private val repo: DetectionRepository
) {
    suspend operator fun invoke(sessionId: String): Result<String> = runCatching {
        repo.exportSessionToCsv(sessionId)
    }
}

class SyncSessionUseCase @Inject constructor(
    private val repo: DetectionRepository
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> =
        repo.syncSessionToCloud(sessionId)
}

class GetDetectionAnalyticsUseCase @Inject constructor(
    private val repo: DetectionRepository
) {
    suspend fun byHour(userId: String, days: Int = 7): Map<Int, Int> =
        repo.getDetectionCountByHour(userId, days)

    suspend fun byDay(userId: String, days: Int = 30): Map<String, Int> =
        repo.getTotalDetectionsByDay(userId, days)
}
