package com.yunok.walzi.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yunok.walzi.MainActivity
import com.yunok.walzi.R
import com.yunok.walzi.presentation.navigation.DeepLinkTarget
import kotlin.random.Random

/**
 * Receives pushes sent from the Walzi admin portal's Notifications dashboard.
 * Every message includes a data payload:
 *   screen        -> "home" | "collections" | "favorites" | "category" | "wallpaper"
 *   wallpaperId   -> present when screen == "wallpaper"
 *   categoryId    -> present when screen == "category"
 *
 * Tapping the notification launches MainActivity with those extras, which
 * AppRoot then uses to navigate to the right screen (see MainActivity + AppRoot).
 */
class WalziFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send this token to your own backend if you want per-user targeting.
        // The admin portal's "device token" test mode expects this exact string
        // to be pasted into its Notifications screen.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Walzi"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val screen = message.data["screen"] ?: DeepLinkTarget.HOME
        val wallpaperId = message.data["wallpaperId"]
        val categoryId = message.data["categoryId"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_DEEP_LINK_SCREEN, screen)
            wallpaperId?.let { putExtra(MainActivity.EXTRA_DEEP_LINK_WALLPAPER_ID, it) }
            categoryId?.let { putExtra(MainActivity.EXTRA_DEEP_LINK_CATEGORY_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
    }
}
