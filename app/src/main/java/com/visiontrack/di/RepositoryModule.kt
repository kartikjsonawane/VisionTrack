package com.visiontrack.di

import com.visiontrack.data.repository.AuthRepositoryImpl
import com.visiontrack.data.repository.DetectionRepositoryImpl
import com.visiontrack.data.repository.UserRepositoryImpl
import com.visiontrack.domain.repository.AuthRepository
import com.visiontrack.domain.repository.DetectionRepository
import com.visiontrack.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindDetectionRepository(impl: DetectionRepositoryImpl): DetectionRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
