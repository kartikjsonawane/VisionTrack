package com.visiontrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.visiontrack.data.local.dao.DetectionDao
import com.visiontrack.data.local.dao.DetectionSessionDao
import com.visiontrack.data.local.entity.DetectedObjectEntity
import com.visiontrack.data.local.entity.DetectionSessionEntity

@Database(
    entities = [
        DetectedObjectEntity::class,
        DetectionSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VisionTrackDatabase : RoomDatabase() {
    abstract fun detectionDao(): DetectionDao
    abstract fun sessionDao(): DetectionSessionDao
}
