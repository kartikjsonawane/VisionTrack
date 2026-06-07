package com.visiontrack.presentation.detection

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.visiontrack.R
import com.visiontrack.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service that keeps detection alive when the app is backgrounded.
 * Required for persistent detection in logistics / surveillance use cases.
 */
@AndroidEntryPoint
class DetectionForegroundService : Service() {

    companion object {
        const val CHANNEL_ID   = "detection_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP  = "com.visiontrack.STOP_DETECTION"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, DetectionForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VisionTrack — Detection Active")
            .setContentText("Real-time object detection is running")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Detection Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "VisionTrack background detection"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
