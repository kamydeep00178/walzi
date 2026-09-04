package com.yunok.walzi

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import com.yunok.walzi.ads.AdManager
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash: a static icon shown for the brief instant before the first Compose
        // frame draws on cold start. It disappears the moment setContent below renders -
        // our own animated SplashScreen composable then takes over as the branded experience.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        requestNotificationPermissionIfNeeded()

        val deepLink = intent?.extras?.let { extras ->
            val screen = extras.getString(EXTRA_DEEP_LINK_SCREEN) ?: return@let null
            DeepLinkTarget(
                screen = screen,
                wallpaperId = extras.getString(EXTRA_DEEP_LINK_WALLPAPER_ID),
                categoryId = extras.getString(EXTRA_DEEP_LINK_CATEGORY_ID)
            )
        }
/*
        ConsentManager.requestConsentInfoUpdate(this) { canRequestAds ->
            if (canRequestAds) {
                AdManager.initialize(this)
            }
            // If canRequestAds = false (user declined GDPR), ads simply won't
            // load — your app still works normally, just without ads.
        }*/

        setContent {
            WalziTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    AppRoot(pendingDeepLink = deepLink)
                }
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: notifications simply won't show if denied */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}