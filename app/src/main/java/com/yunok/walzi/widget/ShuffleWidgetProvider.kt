package com.yunok.walzi.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.yunok.walzi.R
import com.yunok.walzi.worker.ShuffleWorker

/**
 * Home-screen "Shuffle" widget: one tap sets the next wallpaper without opening the app.
 * The tap only enqueues [ShuffleWorker] (a broadcast receiver must finish quickly); the label
 * shows "Changing…" until the worker resets it.
 */
class ShuffleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        manager.updateAppWidget(appWidgetIds, views(context, busy = false))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SHUFFLE) {
            updateAll(context, busy = true)
            ShuffleWorker.enqueue(context)
        }
    }

    companion object {
        private const val ACTION_SHUFFLE = "com.yunok.walzi.widget.SHUFFLE"

        /** Refreshes every placed Shuffle widget (e.g. back to "Shuffle" after a change). */
        fun updateAll(context: Context, busy: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ShuffleWidgetProvider::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views(context, busy))
        }

        private fun views(context: Context, busy: Boolean): RemoteViews {
            val tap = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ShuffleWidgetProvider::class.java).setAction(ACTION_SHUFFLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return RemoteViews(context.packageName, R.layout.widget_shuffle).apply {
                setTextViewText(
                    R.id.widget_label,
                    context.getString(if (busy) R.string.widget_shuffle_busy else R.string.widget_shuffle_label)
                )
                setOnClickPendingIntent(R.id.widget_root, tap)
            }
        }
    }
}
