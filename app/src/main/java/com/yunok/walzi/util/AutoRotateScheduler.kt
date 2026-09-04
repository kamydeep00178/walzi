package com.yunok.walzi.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yunok.walzi.worker.AutoRotateWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues/cancels the daily AutoRotateWorker job. Call schedule() when the user turns
 *  auto-rotate on, cancel() when they turn it off. */
@Singleton
class AutoRotateScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UNIQUE_WORK_NAME = "walzi_auto_rotate_daily"
    }

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<AutoRotateWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}