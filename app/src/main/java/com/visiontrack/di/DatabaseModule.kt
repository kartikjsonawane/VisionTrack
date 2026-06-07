package com.visiontrack.di

import android.content.Context
import androidx.room.Room
import com.visiontrack.data.local.VisionTrackDatabase
import com.visiontrack.data.local.dao.DetectionDao
import com.visiontrack.data.local.dao.DetectionSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VisionTrackDatabase =
        Room.databaseBuilder(context, VisionTrackDatabase::class.java, "visiontrack.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideDetectionDao(db: VisionTrackDatabase): DetectionDao = db.detectionDao()

    @Provides fun provideSessionDao(db: VisionTrackDatabase): DetectionSessionDao = db.sessionDao()
}
