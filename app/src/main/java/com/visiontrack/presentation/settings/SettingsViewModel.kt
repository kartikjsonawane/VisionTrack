package com.visiontrack.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visiontrack.ml.YOLOv8Detector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val confidenceThreshold: Float = 0.25f,
    val iouThreshold: Float        = 0.45f,
    val gpuEnabled: Boolean        = true,
    val trackingEnabled: Boolean   = true,
    val torchEnabled: Boolean      = false,
    val resolution: String         = "1280×720",
    val zoneAlertsEnabled: Boolean = true,
    val hapticEnabled: Boolean     = true,
    val autoSync: Boolean          = true,
    val saveFrames: Boolean        = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val detector: YOLOv8Detector,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val KEY_CONFIDENCE = floatPreferencesKey("confidence_threshold")
        val KEY_IOU        = floatPreferencesKey("iou_threshold")
        val KEY_GPU        = booleanPreferencesKey("gpu_enabled")
        val KEY_TRACKING   = booleanPreferencesKey("tracking_enabled")
        val KEY_TORCH      = booleanPreferencesKey("torch_enabled")
        val KEY_RESOLUTION = stringPreferencesKey("resolution")
        val KEY_ZONE_ALERT = booleanPreferencesKey("zone_alerts")
        val KEY_HAPTIC     = booleanPreferencesKey("haptic_enabled")
        val KEY_AUTO_SYNC  = booleanPreferencesKey("auto_sync")
        val KEY_SAVE_FRAMES= booleanPreferencesKey("save_frames")
    }

    val uiState: StateFlow<SettingsUiState> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            SettingsUiState(
                confidenceThreshold = prefs[KEY_CONFIDENCE] ?: 0.25f,
                iouThreshold        = prefs[KEY_IOU]        ?: 0.45f,
                gpuEnabled          = prefs[KEY_GPU]        ?: true,
                trackingEnabled     = prefs[KEY_TRACKING]   ?: true,
                torchEnabled        = prefs[KEY_TORCH]      ?: false,
                resolution          = prefs[KEY_RESOLUTION] ?: "1280×720",
                zoneAlertsEnabled   = prefs[KEY_ZONE_ALERT] ?: true,
                hapticEnabled       = prefs[KEY_HAPTIC]     ?: true,
                autoSync            = prefs[KEY_AUTO_SYNC]  ?: true,
                saveFrames          = prefs[KEY_SAVE_FRAMES]?: false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setConfidenceThreshold(v: Float) = save(KEY_CONFIDENCE, v) { detector.confidenceThreshold = v }
    fun setIouThreshold(v: Float)        = save(KEY_IOU, v)        { detector.iouThreshold = v }
    fun setGpuEnabled(v: Boolean)        = save(KEY_GPU, v)
    fun setTrackingEnabled(v: Boolean)   = save(KEY_TRACKING, v)
    fun setTorchEnabled(v: Boolean)      = save(KEY_TORCH, v)
    fun setResolution(v: String)         = save(KEY_RESOLUTION, v)
    fun setZoneAlertsEnabled(v: Boolean) = save(KEY_ZONE_ALERT, v)
    fun setHapticEnabled(v: Boolean)     = save(KEY_HAPTIC, v)
    fun setAutoSync(v: Boolean)          = save(KEY_AUTO_SYNC, v)
    fun setSaveFrames(v: Boolean)        = save(KEY_SAVE_FRAMES, v)

    private fun <T> save(key: Preferences.Key<T>, value: T, sideEffect: (() -> Unit)? = null) {
        viewModelScope.launch {
            dataStore.edit { it[key] = value }
            sideEffect?.invoke()
        }
    }
}
