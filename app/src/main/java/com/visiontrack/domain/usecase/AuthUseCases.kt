package com.visiontrack.domain.usecase

import com.visiontrack.domain.model.User
import com.visiontrack.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class AuthUseCases(
    val signIn: SignInUseCase,
    val signUp: SignUpUseCase,
    val signOut: SignOutUseCase,
    val resetPassword: ResetPasswordUseCase,
    val observeAuthState: ObserveAuthStateUseCase
)

class SignInUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        repo.signInWithEmail(email, password)
}

class SignUpUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, name: String): Result<User> =
        repo.signUpWithEmail(email, password, name)
}

class SignOutUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.signOut()
}

class ResetPasswordUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> = repo.resetPassword(email)
}

class ObserveAuthStateUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): Flow<User?> = repo.currentUser
}
