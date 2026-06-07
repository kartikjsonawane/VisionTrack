package com.visiontrack.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visiontrack.domain.model.AuthState
import com.visiontrack.domain.usecase.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    val authState: StateFlow<AuthState> = authUseCases.observeAuthState()
        .map { user ->
            if (user != null) AuthState.Authenticated(user)
            else AuthState.Unauthenticated
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authUseCases.signIn(email, password)
                .onSuccess { user -> onSuccess(user.uid) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun signUp(email: String, password: String, name: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authUseCases.signUp(email, password, name)
                .onSuccess { user -> onSuccess(user.uid) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun signOut() = viewModelScope.launch { authUseCases.signOut() }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
