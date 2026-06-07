package com.visiontrack.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.visiontrack.domain.model.User
import com.visiontrack.domain.repository.AuthRepository
import com.visiontrack.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepo: UserRepository
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val fbUser = firebaseAuth.currentUser
            if (fbUser != null) {
                trySend(
                    User(
                        uid = fbUser.uid,
                        email = fbUser.email ?: "",
                        displayName = fbUser.displayName ?: "User",
                        photoUrl = fbUser.photoUrl?.toString()
                    )
                )
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> =
        runCatching {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val fbUser = result.user ?: error("Authentication failed")
            User(uid = fbUser.uid, email = fbUser.email ?: "", displayName = fbUser.displayName ?: "User")
        }.also { Timber.d("SignIn: ${it.isSuccess}") }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> =
        runCatching {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val fbUser = result.user ?: error("Registration failed")

            // Set display name
            fbUser.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
            ).await()

            val user = User(uid = fbUser.uid, email = email, displayName = displayName)
            userRepo.saveUser(user)
            user
        }.also { Timber.d("SignUp: ${it.isSuccess}") }

    override suspend fun signOut() {
        auth.signOut()
        Timber.d("User signed out")
    }

    override suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun updateProfile(displayName: String, photoUrl: String?): Result<User> =
        runCatching {
            val fbUser = auth.currentUser ?: error("Not authenticated")
            val request = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .apply { photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) } }
                .build()
            fbUser.updateProfile(request).await()
            User(uid = fbUser.uid, email = fbUser.email ?: "", displayName = displayName, photoUrl = photoUrl)
        }

    override fun isAuthenticated(): Boolean = auth.currentUser != null
}
