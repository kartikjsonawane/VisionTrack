package com.visiontrack.presentation.detection

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visiontrack.domain.model.DetectedObject
import com.visiontrack.domain.model.DetectionAlert
import com.visiontrack.domain.model.DetectionSession
import com.visiontrack.domain.model.InferenceMetrics
import com.visiontrack.domain.usecase.DetectionUseCases
import com.visiontrack.ml.ObjectDetectionHelper
import com.visiontrack.ml.YOLOv8Detector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class DetectionUiState(
    val isDetecting: Boolean              = false,
    val currentObjects: List<DetectedObject> = emptyList(),
    val currentSession: DetectionSession? = null,
    val fps: Float                        = 0f,
    val latencyMs: Float                  = 0f,
    val totalDetectionsInSession: Int     = 0,
    val errorMessage: String?             = null,
    val flashEnabled: Boolean             = false,
    val confidenceThreshold: Float        = 0.25f
)

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val useCases: DetectionUseCases,
    private val detectionHelper: ObjectDetectionHelper,
    private val detector: YOLOv8Detector
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectionUiState())
    val uiState: StateFlow<DetectionUiState> = _uiState.asStateFlow()

    val alerts: SharedFlow<DetectionAlert> = detectionHelper.alerts

    // Exposed so LiveDetectionScreen can pass the injected singleton to DetectionAnalyzer
    val helper: ObjectDetectionHelper get() = detectionHelper

    // Pending detection batch to flush to DB every N frames
    private val pendingDetections = mutableListOf<DetectedObject>()
    private var frameCount = 0L
    private var userId = ""

    // Rolling object count map for UI
    private val objectCounts = mutableMapOf<String, Int>()

    fun initialize(uid: String) {
        userId = uid
        _uiState.update { it.copy(confidenceThreshold = detector.confidenceThreshold) }
    }

    fun startDetection() {
        viewModelScope.launch {
            try {
                val session = useCases.startSession(userId)
                _uiState.update { it.copy(isDetecting = true, currentSession = session, totalDetectionsInSession = 0) }
                frameCount = 0
                pendingDetections.clear()
                objectCounts.clear()
                Timber.d("Detection session started: ${session.id}")
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(errorMessage = "Failed to start session: ${e.message}") }
            }
        }
    }

    fun stopDetection() {
        viewModelScope.launch {
            val sessionId = _uiState.value.currentSession?.id ?: return@launch
            val avgFps     = detectionHelper.averageFps
            val avgLatency = detectionHelper.averageLatencyMs
            // Flush remaining detections before closing
            flushDetections()
            try {
                val closed = useCases.endSession(sessionId, avgFps, avgLatency)
                _uiState.update { it.copy(isDetecting = false, currentSession = closed, currentObjects = emptyList()) }
                // Trigger background cloud sync
                useCases.syncSession(sessionId)
                Timber.d("Detection session ended: $sessionId")
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(isDetecting = false, errorMessage = e.message) }
            }
        }
    }

    /**
     * Called by [DetectionAnalyzer] on every processed frame.
     * Runs on the camera analysis thread — posts state updates to main via StateFlow.
     */
    fun onFrameDetected(detections: List<DetectedObject>) {
        val state = _uiState.value
        if (!state.isDetecting) return
        // Don't accumulate if no session is active yet
        val sessionId = state.currentSession?.id ?: return

        frameCount++

        // Stamp the real sessionId — the analyzer creates objects with sessionId=""
        // because the session wasn't started at analyzer construction time.
        val stamped = detections.map { it.copy(sessionId = sessionId) }

        stamped.forEach { objectCounts[it.label] = (objectCounts[it.label] ?: 0) + 1 }

        pendingDetections.addAll(stamped)
        if (frameCount % 10 == 0L) {
            flushDetections()
        }

        val fps       = detectionHelper.averageFps
        val latencyMs = detectionHelper.averageLatencyMs

        _uiState.update { it.copy(
            currentObjects            = stamped,
            fps                       = fps,
            latencyMs                 = latencyMs,
            totalDetectionsInSession  = it.totalDetectionsInSession + stamped.size
        )}
    }

    private fun flushDetections() {
        if (pendingDetections.isEmpty()) return
        val batch = pendingDetections.toList()
        pendingDetections.clear()
        viewModelScope.launch {
            try {
                useCases.saveDetections(batch)
            } catch (e: Exception) {
                Timber.w(e, "Failed to flush detections to DB — non-fatal")
            }
        }
    }

    fun setConfidenceThreshold(value: Float) {
        detector.confidenceThreshold = value
        _uiState.update { it.copy(confidenceThreshold = value) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
