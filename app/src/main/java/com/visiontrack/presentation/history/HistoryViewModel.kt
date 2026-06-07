package com.visiontrack.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visiontrack.domain.model.DetectionSession
import com.visiontrack.domain.usecase.DetectionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<DetectionSession> = emptyList(),
    val isLoading: Boolean               = true,
    val exportPath: String?              = null,
    val error: String?                   = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val useCases: DetectionUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun loadHistory(userId: String) {
        viewModelScope.launch {
            useCases.getSessionHistory(userId)
                .onEach { sessions ->
                    _uiState.update { it.copy(sessions = sessions, isLoading = false) }
                }
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect()
        }
    }

    fun exportSession(sessionId: String) {
        viewModelScope.launch {
            useCases.exportSessionCsv(sessionId)
                .onSuccess { path -> _uiState.update { it.copy(exportPath = path) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun syncSession(sessionId: String) {
        viewModelScope.launch {
            useCases.syncSession(sessionId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            // TODO: wire deleteSession use case
            _uiState.update { it.copy(sessions = it.sessions.filter { s -> s.id != sessionId }) }
        }
    }

    fun clearExportPath() = _uiState.update { it.copy(exportPath = null) }
}
