package com.yunok.walzi.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.yunok.walzi.BuildConfig

/** Thin wrapper so the rest of the app never touches Crashlytics directly (and never crashes
 *  because of it). Collection is disabled for debug builds in [com.yunok.walzi.WalziApp]. */
object CrashReporter {
    private const val TAG = "Walzi"

    /** Records a handled-but-unexpected exception as a non-fatal. */
    fun record(t: Throwable) {
        Log.w(TAG, "Unhandled error", t)
        if (BuildConfig.DEBUG) return
        try {
            FirebaseCrashlytics.getInstance().recordException(t)
        } catch (_: Exception) {
        }
    }
}
