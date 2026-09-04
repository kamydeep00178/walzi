package com.yunok.walzi.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.yunok.walzi.BuildConfig

/**
 * ConsentManager — wraps Google UMP SDK.
 *
 * WHO SEES A CONSENT FORM?
 * ┌─────────────────────────────────┬──────────────────────────────────────┐
 * │ User location                   │ What happens                         │
 * ├─────────────────────────────────┼──────────────────────────────────────┤
 * │ EEA, UK, Switzerland (GDPR)     │ GDPR consent popup shown             │
 * │ California, Virginia, CO, etc.  │ "Do Not Sell My Data" form shown     │
 * │ India, Australia, Canada, etc.  │ NO form shown — ads load immediately │
 * └─────────────────────────────────┴──────────────────────────────────────┘
 *
 * HOW canRequestAds BECOMES TRUE:
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ • Non-regulated country (e.g. India) → true immediately, no form shown  │
 * │ • GDPR user → true after they tap "Accept" or "Accept all" on the form  │
 * │ • GDPR user → false if they decline all (ads won't load)                │
 * │ • US user   → true immediately (opt-OUT not opt-IN, so ads show first)  │
 * │ • Already consented on a previous launch → true immediately, no form    │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
object ConsentManager {

    private const val TAG = "ConsentManager"

    /**
     * Call from MainActivity.onCreate() — BEFORE MobileAds.initialize().
     *
     * onComplete(canRequestAds: Boolean) fires when:
     *  - The consent form is dismissed (accepted/declined)
     *  - No form was needed (non-regulated country or already consented)
     *  - There was an error (we still try to serve ads)
     */
    fun requestConsentInfoUpdate(
        activity   : Activity,
        onComplete : (canRequestAds: Boolean) -> Unit
    ) {
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        // ── DEBUG ONLY — simulates different regions for testing ───────────────
        // Remove or comment out before release build!
        // Uncomment ONE of these to test the consent form on your debug device:
        //
        // Test EEA/GDPR form:
        // if (BuildConfig.DEBUG) {
        //     val debugSettings = ConsentDebugSettings.Builder(activity)
        //         .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        //         .addTestDeviceHashedId("YOUR_DEVICE_HASH_FROM_LOGCAT")
        //         .build()
        //     paramsBuilder.setConsentDebugSettings(debugSettings)
        // }
        //
        // Test non-EEA (no form shown):
        // if (BuildConfig.DEBUG) {
        //     val debugSettings = ConsentDebugSettings.Builder(activity)
        //         .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA)
        //         .addTestDeviceHashedId("YOUR_DEVICE_HASH_FROM_LOGCAT")
        //         .build()
        //     paramsBuilder.setConsentDebugSettings(debugSettings)
        // }
        // ──────────────────────────────────────────────────────────────────────

        val params = paramsBuilder.build()
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        consentInfo.requestConsentInfoUpdate(
            activity, params,
            {
                Log.d(
                    TAG,
                    "status=${consentInfo.consentStatus}, " +
                            "privacy=${consentInfo.privacyOptionsRequirementStatus}, " +
                            "canAds=${consentInfo.canRequestAds()}"
                )

                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    Log.d(
                        TAG,
                        "FORM RESULT = ${formError?.message ?: "NO ERROR"}"
                    )

                    Log.d(
                        TAG,
                        "AFTER FORM: status=${consentInfo.consentStatus}, " +
                                "privacy=${consentInfo.privacyOptionsRequirementStatus}, " +
                                "canAds=${consentInfo.canRequestAds()}"
                    )

                    if (consentInfo.canRequestAds()) {
                        onComplete(true)
                    }
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info error: ${requestError.message}")
                val canRequest = consentInfo.canRequestAds()
                Log.d(TAG, "canRequestAds = $canRequest (after error)")
                onComplete(canRequest)
            }
        )
    }

    /**
     * For SettingsScreen — shows the privacy options form so users can
     * change their consent at any time (required by GDPR).
     * Show a "Privacy Settings" button only when this returns true.
     */
    fun isPrivacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context)
            .privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptionsForm(activity: Activity, onDismiss: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            onDismiss()
        }
    }

    /** Reset consent — useful for testing. Call from debug menu only. */
    fun resetForTesting(context: Context) {
        if (BuildConfig.DEBUG) {
            UserMessagingPlatform.getConsentInformation(context).reset()
            Log.d(TAG, "Consent reset for testing")
        }
    }
}

