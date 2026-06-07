package com.visiontrack.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.visiontrack.data.local.dao.DetectionDao
import com.visiontrack.data.local.dao.DetectionSessionDao
import com.visiontrack.data.mapper.*
import com.visiontrack.di.IoDispatcher
import com.visiontrack.domain.model.DetectedObject
import com.visiontrack.domain.model.DetectionSession
import com.visiontrack.domain.model.ObjectFrequency
import com.visiontrack.domain.repository.DetectionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class DetectionRepositoryImpl @Inject constructor(
    private val detectionDao: DetectionDao,
    private val sessionDao: DetectionSessionDao,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DetectionRepository {

    override suspend fun startSession(userId: String): DetectionSession = withContext(ioDispatcher) {
        val session = DetectionSession(
            id = UUID.randomUUID().toString(),
            userId = userId,
            startTimeMs = System.currentTimeMillis()
        )
        sessionDao.insertSession(session.toEntity())
        session
    }

    override suspend fun endSession(
        sessionId: String,
        avgFps: Float,
        avgLatencyMs: Float
    ): DetectionSession = withContext(ioDispatcher) {
        val detections = detectionDao.getDetectionsForExport(sessionId)
        val count = detections.size
        val uniqueLabels = detections.map { it.label }.toSet().toList()
        val labelsJson = com.google.gson.Gson().toJson(uniqueLabels)
        val now = System.currentTimeMillis()
        sessionDao.closeSession(sessionId, now, count, avgFps, avgLatencyMs, labelsJson)
        sessionDao.getSessionById(sessionId)!!.toDomain()
    }

    override fun getSessionsForUser(userId: String): Flow<List<DetectionSession>> =
        sessionDao.getSessionsForUser(userId).map { it.map { e -> e.toDomain() } }

    override suspend fun getSessionById(sessionId: String): DetectionSession? =
        withContext(ioDispatcher) { sessionDao.getSessionById(sessionId)?.toDomain() }

    override suspend fun deleteSession(sessionId: String) = withContext(ioDispatcher) {
        sessionDao.deleteSessionById(sessionId)
    }

    override suspend fun saveDetection(obj: DetectedObject) = withContext(ioDispatcher) {
        val userId = sessionDao.getSessionById(obj.sessionId)?.userId ?: ""
        detectionDao.insertDetection(obj.toEntity(userId))
    }

    override suspend fun saveDetections(objects: List<DetectedObject>) = withContext(ioDispatcher) {
        if (objects.isEmpty()) return@withContext
        // Derive userId from the session — DetectedObject domain model doesn't carry userId.
        // toEntity's parameter is userId, not sessionId; look it up once per batch.
        val sessionId = objects.first().sessionId
        val userId = sessionDao.getSessionById(sessionId)?.userId ?: ""
        detectionDao.insertDetections(objects.map { it.toEntity(userId) })
    }

    override fun getDetectionsForSession(sessionId: String): Flow<List<DetectedObject>> =
        detectionDao.getDetectionsForSession(sessionId).map { it.map { e -> e.toDomain() } }

    override fun getAllDetectionsForUser(userId: String): Flow<List<DetectedObject>> =
        detectionDao.getAllDetectionsForUser(userId).map { it.map { e -> e.toDomain() } }

    override suspend fun getDetectionCount(userId: String): Int = withContext(ioDispatcher) {
        detectionDao.getDetectionCount(userId)
    }

    override suspend fun getObjectFrequencies(userId: String, limit: Int): List<ObjectFrequency> =
        withContext(ioDispatcher) {
            detectionDao.getObjectFrequencies(userId, limit).map { it.toDomain() }
        }

    override suspend fun exportSessionToCsv(sessionId: String): String = withContext(ioDispatcher) {
        val detections = detectionDao.getDetectionsForExport(sessionId)
        val dir = File(context.getExternalFilesDir(null), "exports").also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "visiontrack_export_$timestamp.csv")

        file.bufferedWriter().use { writer ->
            writer.write("id,label,confidence,left,top,right,bottom,trackingId,timestampMs,zoneId\n")
            detections.forEach { d ->
                writer.write(
                    "${d.id},${d.label},${d.confidence}," +
                    "${d.boundingBoxLeft},${d.boundingBoxTop},${d.boundingBoxRight},${d.boundingBoxBottom}," +
                    "${d.trackingId ?: ""},${d.timestampMs},${d.zoneId ?: ""}\n"
                )
            }
        }
        Timber.d("Exported ${detections.size} detections to ${file.absolutePath}")
        file.absolutePath
    }

    override suspend fun syncSessionToCloud(sessionId: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val session = sessionDao.getSessionById(sessionId) ?: return@withContext
            val detections = detectionDao.getDetectionsForExport(sessionId)

            // Write session document
            firestore.collection("sessions").document(sessionId)
                .set(session).await()

            // Write detections as sub-collection in batches of 500
            detections.chunked(500).forEach { batch ->
                val firestoreBatch = firestore.batch()
                batch.forEach { d ->
                    val ref = firestore.collection("sessions").document(sessionId)
                        .collection("detections").document(d.id)
                    firestoreBatch.set(ref, d)
                }
                firestoreBatch.commit().await()
            }

            sessionDao.markSynced(sessionId)
            Timber.d("Synced session $sessionId with ${detections.size} detections")
        }
    }

    override suspend fun getDetectionCountByHour(userId: String, days: Int): Map<Int, Int> =
        withContext(ioDispatcher) {
            val sinceMs = System.currentTimeMillis() - days * 86_400_000L
            detectionDao.getDetectionCountByHour(userId, sinceMs)
                .associate { it.hour.toInt() to it.count }
        }

    override suspend fun getTotalDetectionsByDay(userId: String, days: Int): Map<String, Int> =
        withContext(ioDispatcher) {
            val sinceMs = System.currentTimeMillis() - days * 86_400_000L
            detectionDao.getDetectionCountByDay(userId, sinceMs)
                .associate { it.day to it.count }
        }
}
