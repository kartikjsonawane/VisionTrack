package com.visiontrack.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visiontrack.domain.model.User
import com.visiontrack.domain.repository.UserRepository
import com.visiontrack.domain.usecase.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User?        = null,
    val isLoading: Boolean = true,
    val error: String?     = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            userRepo.observeUser(userId)
                .onEach  { user -> _uiState.update { it.copy(user = user, isLoading = false) } }
                .catch   { e   -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect()
        }
    }

    fun signOut() = viewModelScope.launch { authUseCases.signOut() }
}
