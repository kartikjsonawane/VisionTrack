package com.visiontrack.domain.repository

import com.visiontrack.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User>
    suspend fun signOut()
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateProfile(displayName: String, photoUrl: String?): Result<User>
    fun isAuthenticated(): Boolean
}
