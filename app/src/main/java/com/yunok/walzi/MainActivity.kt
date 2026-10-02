package com.yunok.walzi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yunok.walzi.ads.AdManager
import com.yunok.walzi.ads.AdsConfig
import com.yunok.walzi.ads.ConsentManager
import com.yunok.walzi.presentation.navigation.AppRoot
import com.yunok.walzi.presentation.navigation.DeepLinkTarget
import com.yunok.walzi.presentation.splash.SplashScreen
import com.yunok.walzi.presentation.theme.WalziTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DEEP_LINK_SCREEN = "deep_link_screen"
        const val EXTRA_DEEP_LINK_WALLPAPER_ID = "deep_link_wallpaper_id"
        const val EXTRA_DEEP_LINK_CATEGORY_ID = "deep_link_category_id"

        private const val PREFS_NAME = "walzi_prefs"
        private const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
    }

    /** A notification tap waiting to be applied by AppRoot; cleared once consumed. */
    private var pendingDeepLink by mutableStateOf<DeepLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash: a static icon shown for the brief instant before the first Compose
        // frame draws on cold start. It disappears the moment setContent below renders -
        // our own animated SplashScreen composable then takes over as the branded experience.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        // Only a fresh launch carries a link to act on. A re-created Activity (process death)
        // still sees the original intent, and re-applying it would yank the user back to the
        // notification's target.
        if (savedInstanceState == null) pendingDeepLink = intent.toDeepLinkTarget()

        // Everything ads-related hangs off this one flag - see AdsConfig. When it's false the
        // consent form is never shown and the SDK is never initialised.
        if (AdsConfig.ADS_ENABLED) {
            ConsentManager.requestConsentInfoUpdate(this) { canRequestAds ->
                // If the user declined (GDPR), ads simply won't load - the app works normally.
                if (canRequestAds) AdManager.initialize(this)
            }
        }

        setContent {
            WalziTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    AppRoot(
                        pendingDeepLink = pendingDeepLink,
                        onDeepLinkConsumed = { pendingDeepLink = null }
                    )
                }
            }
        }
    }

    /** launchMode is singleTask: tapping a notification while the app is open lands here, not onCreate. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toDeepLinkTarget()?.let { pendingDeepLink = it }
    }

    private fun Intent?.toDeepLinkTarget(): DeepLinkTarget? {
        val extras = this?.extras ?: return null
        val screen = extras.getString(EXTRA_DEEP_LINK_SCREEN) ?: return null
        return DeepLinkTarget(
            screen = screen,
            wallpaperId = extras.getString(EXTRA_DEEP_LINK_WALLPAPER_ID),
            categoryId = extras.getString(EXTRA_DEEP_LINK_CATEGORY_ID)
        )
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: notifications simply won't show if denied */ }

    /** Asks once. Re-prompting on every cold start is annoying and Android stops showing it after two denials anyway. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) return

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false)) return
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, true).apply()

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
