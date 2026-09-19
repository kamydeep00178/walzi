package com.yunok.walzi.presentation.notifications

import com.yunok.walzi.data.local.entity.NotificationEntity
import com.yunok.walzi.presentation.common.MviEffect
import com.yunok.walzi.presentation.common.MviIntent
import com.yunok.walzi.presentation.common.MviState
import com.yunok.walzi.presentation.navigation.DeepLinkTarget

data class NotificationsState(
    val notifications: List<NotificationEntity> = emptyList(),
    val isLoading: Boolean = true
) : MviState

sealed interface NotificationsIntent : MviIntent {
    data class NotificationTapped(val notification: NotificationEntity) : NotificationsIntent
}

sealed interface NotificationsEffect : MviEffect {
    data class Navigate(val target: DeepLinkTarget) : NotificationsEffect
}