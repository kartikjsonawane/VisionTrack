package com.visiontrack.domain.model

data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val totalDetections: Int = 0,
    val totalSessions: Int = 0,
    val planType: PlanType = PlanType.FREE
)

enum class PlanType { FREE, PRO, ENTERPRISE }

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
