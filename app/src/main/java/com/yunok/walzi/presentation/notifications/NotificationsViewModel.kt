package com.yunok.walzi.presentation.notifications

import androidx.lifecycle.viewModelScope
import com.yunok.walzi.data.local.dao.NotificationDao
import com.yunok.walzi.presentation.common.BaseViewModel
import com.yunok.walzi.presentation.navigation.DeepLinkTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationDao: NotificationDao
) : BaseViewModel<NotificationsIntent, NotificationsState, NotificationsEffect>(NotificationsState()) {

    init {
        viewModelScope.launch {
            notificationDao.observeAll().collect { notifications ->
                setState { copy(notifications = notifications, isLoading = false) }
            }
        }
    }

    override suspend fun handleIntent(intent: NotificationsIntent) {
        when (intent) {
            is NotificationsIntent.NotificationTapped -> {
                notificationDao.markAsRead(intent.notification.id)
                setEffect(
                    NotificationsEffect.Navigate(
                        DeepLinkTarget(
                            screen = intent.notification.screen,
                            wallpaperId = intent.notification.wallpaperId,
                            categoryId = intent.notification.categoryId
                        )
                    )
                )
            }
        }
    }
}