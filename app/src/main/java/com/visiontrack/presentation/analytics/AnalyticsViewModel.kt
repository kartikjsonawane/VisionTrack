package com.visiontrack.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visiontrack.domain.model.ObjectFrequency
import com.visiontrack.domain.usecase.DetectionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean               = true,
    val totalDetections: Int             = 0,
    val topObjects: List<ObjectFrequency> = emptyList(),
    val hourlyData: Map<Int, Int>        = emptyMap(),
    val dailyData: Map<String, Int>      = emptyMap(),
    val avgLatencyMs: Float              = 0f,
    val error: String?                   = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val useCases: DetectionUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    fun loadAnalytics(userId: String) {
        viewModelScope.launch {
            try {
                val topObjects   = useCases.getObjectFrequencies(userId, limit = 10)
                val hourlyData   = useCases.getAnalytics.byHour(userId, days = 7)
                val dailyData    = useCases.getAnalytics.byDay(userId, days = 30)
                val totalDetections = topObjects.sumOf { it.count }

                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        totalDetections = totalDetections,
                        topObjects      = topObjects,
                        hourlyData      = hourlyData,
                        dailyData       = dailyData
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
