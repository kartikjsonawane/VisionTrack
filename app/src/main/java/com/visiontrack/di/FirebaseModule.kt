package com.visiontrack.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides @Singleton fun provideFirestore(): FirebaseFirestore =
        Firebase.firestore.also { db ->
            db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }

    @Provides @Singleton fun provideFirebaseStorage(): FirebaseStorage = Firebase.storage

    @Provides @Singleton fun provideFirebaseAnalytics(): FirebaseAnalytics = Firebase.analytics

    @Provides @Singleton fun provideFirebaseCrashlytics(): FirebaseCrashlytics = Firebase.crashlytics

    @Provides @Singleton fun provideFirebaseMessaging(): FirebaseMessaging = Firebase.messaging
}
