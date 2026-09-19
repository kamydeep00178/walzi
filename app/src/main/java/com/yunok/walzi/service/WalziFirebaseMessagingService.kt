package com.yunok.walzi.service

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yunok.walzi.MainActivity
import com.yunok.walzi.R
import com.yunok.walzi.data.local.dao.NotificationDao
import com.yunok.walzi.data.local.entity.NotificationEntity
import com.yunok.walzi.presentation.navigation.DeepLinkTarget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class WalziFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationDao: NotificationDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Walzi"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val screen = message.data["screen"] ?: DeepLinkTarget.HOME
        val wallpaperId = message.data["wallpaperId"]
        val categoryId = message.data["categoryId"]
        val imageUrl = message.data["imageUrl"]

        serviceScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    id = message.messageId ?: UUID.randomUUID().toString(),
                    title = title,
                    body = body,
                    imageUrl = imageUrl,
                    screen = screen,
                    wallpaperId = wallpaperId,
                    categoryId = categoryId,
                    receivedAt = System.currentTimeMillis()
                )
            )
        }

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