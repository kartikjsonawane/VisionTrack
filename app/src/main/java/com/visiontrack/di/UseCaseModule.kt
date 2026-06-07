package com.visiontrack.di

import com.visiontrack.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides @Singleton
    fun provideDetectionUseCases(
        startSession: StartDetectionSessionUseCase,
        endSession: EndDetectionSessionUseCase,
        saveDetections: SaveDetectionsUseCase,
        getHistory: GetSessionHistoryUseCase,
        getFrequencies: GetObjectFrequenciesUseCase,
        exportCsv: ExportSessionCsvUseCase,
        sync: SyncSessionUseCase,
        analytics: GetDetectionAnalyticsUseCase
    ): DetectionUseCases = DetectionUseCases(
        startSession, endSession, saveDetections, getHistory,
        getFrequencies, exportCsv, sync, analytics
    )

    @Provides @Singleton
    fun provideAuthUseCases(
        signIn: SignInUseCase,
        signUp: SignUpUseCase,
        signOut: SignOutUseCase,
        resetPassword: ResetPasswordUseCase,
        observeAuth: ObserveAuthStateUseCase
    ): AuthUseCases = AuthUseCases(signIn, signUp, signOut, resetPassword, observeAuth)
}
