package com.visiontrack.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.visiontrack.di.IoDispatcher
import com.visiontrack.domain.model.User
import com.visiontrack.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserRepository {

    private fun usersCollection() = firestore.collection("users")

    override suspend fun getUser(uid: String): User? = withContext(ioDispatcher) {
        runCatching {
            usersCollection().document(uid).get().await()
                .toObject(UserDocument::class.java)?.toDomain()
        }.getOrNull()
    }

    override fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val listener = usersCollection().document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Timber.e(error); return@addSnapshotListener }
                trySend(snapshot?.toObject(UserDocument::class.java)?.toDomain())
            }
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    override suspend fun saveUser(user: User): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            usersCollection().document(user.uid).set(user.toDocument()).await()
        }
    }

    override suspend fun updateUserStats(uid: String, detectionsDelta: Int, sessionsDelta: Int): Result<Unit> =
        runCatching {
            withContext(ioDispatcher) {
                usersCollection().document(uid).update(
                    mapOf(
                        "totalDetections" to com.google.firebase.firestore.FieldValue.increment(detectionsDelta.toLong()),
                        "totalSessions" to com.google.firebase.firestore.FieldValue.increment(sessionsDelta.toLong())
                    )
                ).await()
            }
        }

    // Firestore DTO
    private data class UserDocument(
        val uid: String = "",
        val email: String = "",
        val displayName: String = "",
        val photoUrl: String? = null,
        val createdAt: Long = 0L,
        val totalDetections: Int = 0,
        val totalSessions: Int = 0,
        val planType: String = "FREE"
    ) {
        fun toDomain() = User(
            uid = uid, email = email, displayName = displayName,
            photoUrl = photoUrl, createdAt = createdAt,
            totalDetections = totalDetections, totalSessions = totalSessions,
            planType = com.visiontrack.domain.model.PlanType.valueOf(planType)
        )
    }

    private fun User.toDocument() = UserDocument(
        uid = uid, email = email, displayName = displayName,
        photoUrl = photoUrl, createdAt = createdAt,
        totalDetections = totalDetections, totalSessions = totalSessions,
        planType = planType.name
    )
}
