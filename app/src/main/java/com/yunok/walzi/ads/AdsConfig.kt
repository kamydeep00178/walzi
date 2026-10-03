package com.yunok.walzi.ads

import com.google.android.gms.ads.RequestConfiguration

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  ONE SWITCH FOR ALL ADS                                                  ║
 * ║                                                                          ║
 * ║  ADS_ENABLED = true   consent form (UMP) + AdMob init + all 4 formats:   ║
 * ║                        - banner: bottom of Home, Category, Favorites,    ║
 * ║                          List detail (adaptive, never on full preview)   ║
 * ║                        - native: in the Home feeds + Category grid       ║
 * ║                          (after 8, then every 12), one in Collections,   ║
 * ║                          one in Notifications                            ║
 * ║                        - interstitial: after Set Wallpaper / Download,   ║
 * ║                          and when opening a category (capped, see below) ║
 * ║                        - rewarded (always opt-in): "Remove ads for 24h"  ║
 * ║                          in Settings, and Download in full preview       ║
 * ║                                                                          ║
 * ║  ADS_ENABLED = false  none of the above runs: no SDK init, no ad         ║
 * ║                       requests, no ad views, Download is free. (The UMP  ║
 * ║                       consent check still runs - it sets Analytics       ║
 * ║                       consent - see ConsentManager.)                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * Every ads entry point checks this flag itself, so call sites never need to.
 *
 * Debug builds always use Google's test ad units regardless of this flag's value; release
 * builds use the real units in [AdManager].
 *
 * NOTE: the manifest keeps its AdMob APPLICATION_ID meta-data even when this is false - the
 * Ads SDK is still on the classpath and crashes at startup if that entry is missing.
 */
object AdsConfig {
    // FIRST RELEASE: false - no real AdMob app/unit ids exist yet.
    // To turn ads on: put the 4 real unit ids in AdManager.kt, the app id in gradle.properties
    // (ADMOB_APP_ID_RELEASE), then set this to true.
    const val ADS_ENABLED: Boolean = true

    /**
     * Highest ad content rating AdMob may serve. PG keeps ads suitable for a broad audience
     * (the app has Cute/Kawaii, Anime and Animals categories). Set the same value in the AdMob
     * console (Blocking controls -> Content rating) so the two never disagree.
     */
    const val MAX_AD_CONTENT_RATING: String = RequestConfiguration.MAX_AD_CONTENT_RATING_PG

    /**
     * Your own phones, so they get test ads even in release builds (clicking your own real ads
     * is invalid traffic and can get the AdMob account suspended). Find the id in Logcat:
     * `Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("33BE2250B43518CCDA7DE426D04EE231"))`
     */
    val TEST_DEVICE_IDS: List<String> = listOf(
        "C0DD1AD5EC853582B7FBF3A8007567F1", // owner's phone (DN2101)
    )

    /**
     * DEBUG builds only (ignored in release): makes the test devices above act as if they were
     * in the EEA, to test the GDPR consent form and Consent Mode. Keep false normally.
     */
    const val DEBUG_FORCE_EEA: Boolean = false
}
