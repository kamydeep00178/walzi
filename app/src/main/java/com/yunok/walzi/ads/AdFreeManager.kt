package com.yunok.walzi.ads

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * The "Remove ads for 24 hours" reward. While active, banners and native ads are hidden and
 * interstitials are skipped. Persisted, so it survives app restarts; [isAdFree] flips back to
 * false by itself when the time runs out.
 */
object AdFreeManager {

    val AD_FREE_DURATION_MS: Long = TimeUnit.HOURS.toMillis(24)

    private const val PREFS_NAME = "ad_free_prefs"
    private const val KEY_AD_FREE_UNTIL = "ad_free_until"

    private var prefs: SharedPreferences? = null
    @Volatile private var adFreeUntil = 0L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var expiryJob: Job? = null

    private val _isAdFree = MutableStateFlow(false)
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    /** Call once from Application.onCreate. */
    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        adFreeUntil = p.getLong(KEY_AD_FREE_UNTIL, 0L)
        refresh()
    }

    fun isAdFreeNow(): Boolean = System.currentTimeMillis() < adFreeUntil

    fun remainingMs(): Long = (adFreeUntil - System.currentTimeMillis()).coerceAtLeast(0L)

    /** Called only after the user has earned a rewarded ad's reward. */
    fun grant() {
        adFreeUntil = System.currentTimeMillis() + AD_FREE_DURATION_MS
        prefs?.edit()?.putLong(KEY_AD_FREE_UNTIL, adFreeUntil)?.apply()
        refresh()
    }

    private fun refresh() {
        val active = isAdFreeNow()
        _isAdFree.value = active
        expiryJob?.cancel()
        if (active) {
            expiryJob = scope.launch {
                delay(remainingMs())
                _isAdFree.value = false
            }
        }
    }
}
