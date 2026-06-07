package com.visiontrack.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.visiontrack.domain.repository.DetectionRepository
import com.visiontrack.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userId: String?               = null,
    val displayName: String           = "User",
    val totalDetections: Int          = 0,
    val totalSessions: Int            = 0,
    val statsCards: List<StatCardData> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepo: UserRepository,
    private val detectionRepo: DetectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            _uiState.update { it.copy(userId = uid) }
            loadUserData(uid)
        }
    }

    private fun loadUserData(uid: String) {
        // Display name from Firestore
        viewModelScope.launch {
            userRepo.observeUser(uid).collect { user ->
                _uiState.update { it.copy(displayName = user?.displayName ?: "User") }
            }
        }
        // Session count and detection count from Room — these are the source of truth
        viewModelScope.launch {
            detectionRepo.getSessionsForUser(uid).collect { sessions ->
                val detectionCount = detectionRepo.getDetectionCount(uid)
                val sessionCount   = sessions.size
                _uiState.update { state ->
                    state.copy(
                        totalDetections = detectionCount,
                        totalSessions   = sessionCount,
                        statsCards      = buildStatsCards(detectionCount, sessionCount)
                    )
                }
            }
        }
    }

    private fun buildStatsCards(detections: Int, sessions: Int) = listOf(
        StatCardData("Detections", "$detections", Icons.Default.Visibility,     Color(0xFF4FC3F7)),
        StatCardData("Sessions",   "$sessions",   Icons.Default.VideoLibrary,   Color(0xFFFFB74D)),
        StatCardData("Avg FPS",    "28",           Icons.Default.Speed,          Color(0xFF80CBC4)),
        StatCardData("Accuracy",   "94%",          Icons.Default.CheckCircle,    Color(0xFF81C784))
    )
}
