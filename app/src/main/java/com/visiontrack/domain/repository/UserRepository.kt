package com.visiontrack.domain.repository

import com.visiontrack.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(uid: String): User?
    fun observeUser(uid: String): Flow<User?>
    suspend fun saveUser(user: User): Result<Unit>
    suspend fun updateUserStats(uid: String, detectionsDelta: Int, sessionsDelta: Int): Result<Unit>
}
