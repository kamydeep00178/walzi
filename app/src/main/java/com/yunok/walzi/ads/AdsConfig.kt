package com.yunok.walzi.ads

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  ONE SWITCH FOR ALL ADS                                                  ║
 * ║                                                                          ║
 * ║  ADS_ENABLED = true   consent form (UMP) + AdMob init + banner (Home)    ║
 * ║                       + native ad (Home feed) + interstitial (after a    ║
 * ║                       wallpaper is set / downloaded, frequency-capped)   ║
 * ║                                                                          ║
 * ║  ADS_ENABLED = false  none of the above runs: no consent prompt, no SDK  ║
 * ║                       init, no ad requests, no ad views, and the         ║
 * ║                       "Ad privacy options" row is hidden in Settings.    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * Every ads entry point (ConsentManager, AdManager, InterstitialAdManager, AdViewModel,
 * BannerAdComposable, NativeAdCard) checks this flag itself, so call sites never need to.
 *
 * Debug builds always use Google's test ad units regardless of this flag's value; release
 * builds use the real units in [AdManager].
 *
 * NOTE: the manifest keeps its AdMob APPLICATION_ID meta-data even when this is false - the
 * Ads SDK is still on the classpath and crashes at startup if that entry is missing.
 * To drop the SDK entirely, remove `play-services-ads` and the AD_ID permission as well.
 */
object AdsConfig {
    // FIRST RELEASE: false - no real AdMob app/unit ids exist yet.
    // To turn ads on later: create the AdMob app + 3 units, put the unit ids in AdManager.kt,
    // put the app id in gradle.properties (ADMOB_APP_ID_RELEASE), then set this to true.
    const val ADS_ENABLED: Boolean = false
}
