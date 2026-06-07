package com.visiontrack.data.remote.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class VisionTrackMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM message received: ${message.notification?.title}")
        // Route to notification builder
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed: $token")
        // Send token to backend
    }
}
