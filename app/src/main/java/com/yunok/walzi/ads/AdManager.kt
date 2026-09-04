package com.yunok.walzi.ads

import android.content.Context
//import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Central place for all AdMob configuration.
 *
 * HOW TO REPLACE TEST IDs:
 * 1. Create your app in AdMob console (admob.google.com)
 * 2. Create ad units:
 *    - Banner ad    → copy the ad unit ID
 *    - Interstitial → copy the ad unit ID
 * 3. Replace the RELEASE_* constants below with your real IDs
 * 4. Also replace admobAppId in build.gradle.kts with your real App ID
 */
object AdManager {

    private lateinit var config: AdsConfig

    private const val TAG = "AdManager"

    // ── Test IDs (Google's official test IDs — safe to use in debug) ─────────
    private const val TEST_BANNER_ID        = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_ID  = "ca-app-pub-3940256099942544/1033173712"

    private const val TEST_NATIVE_ID        = "ca-app-pub-3940256099942544/2247696110"

    // ── Release IDs — REPLACE THESE with your real AdMob unit IDs ────────────
    private const val RELEASE_BANNER_ID       = "ca-app-pub-4136650480208705/9399995791"
    private const val RELEASE_INTERSTITIAL_ID = "ca-app-pub-4136650480208705/6857592938"

    private const val RELEASE_NATIVE_ID = "ca-app-pub-4136650480208705/4661840286"

    fun init(config: AdsConfig) {
        this.config = config
    }


    // ── Active IDs chosen at build time ───────────────────────────────────────
    val BANNER_AD_UNIT_ID : String
        get() = if (config.isDebug)
            TEST_BANNER_ID else TEST_BANNER_ID

    val INTERSTITIAL_AD_UNIT_ID : String
        get() = if (config.isDebug)
            TEST_INTERSTITIAL_ID else TEST_INTERSTITIAL_ID

    val NATIVE_AD_UNIT_ID : String
        get() = if (config.isDebug)
            TEST_NATIVE_ID else RELEASE_NATIVE_ID

    // ── Initialization ────────────────────────────────────────────────────────
    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {

            // ✅ ADD THIS BLOCK
            /*val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf("824B3851725BABC5B7090795A20C2634"))
                .build()

            MobileAds.setRequestConfiguration(configuration)*/

           /* MobileAds.initialize(context) { initStatus ->
                val statusMap = initStatus.adapterStatusMap
                for ((adapter, status) in statusMap) {
                    Log.d(TAG, "Adapter: $adapter — ${status.initializationState}")
                }
                Log.d(TAG, "AdMob initialized ✅")
            }*/
        }
    }
}