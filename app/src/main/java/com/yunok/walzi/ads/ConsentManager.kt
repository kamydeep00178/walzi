package com.yunok.walzi.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.firebase.analytics.FirebaseAnalytics
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

        // DEBUG builds only: pretend this device is in the EEA so the GDPR form and the
        // denied-by-default Analytics consent can be tested. Never active in release builds.
        if (BuildConfig.DEBUG && AdsConfig.DEBUG_FORCE_EEA) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .apply { AdsConfig.TEST_DEVICE_IDS.forEach { addTestDeviceHashedId(it) } }
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        val params = paramsBuilder.build()
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        // Returning user who already gave consent: start the SDK right away instead of waiting
        // for the network round-trip below (Google's recommended UMP flow).
        if (consentInfo.canRequestAds()) onComplete(true)

        consentInfo.requestConsentInfoUpdate(
            activity, params,
            {
                Log.d(
                    TAG,
                    "status=${consentInfo.consentStatus}, " +
                            "privacy=${consentInfo.privacyOptionsRequirementStatus}, " +
                            "canAds=${consentInfo.canRequestAds()}"
                )

                // Status is known now (not required / already obtained) - update Analytics before any form.
                applyAnalyticsConsent(activity)

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

                    // The user just made (or kept) their choice - pass it to Analytics.
                    applyAnalyticsConsent(activity)

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
            // The user may have changed their choice - keep Analytics in sync.
            applyAnalyticsConsent(activity)
            onDismiss()
        }
    }

    /**
     * Google Consent Mode for Firebase Analytics. The manifest starts every type DENIED; this
     * sets the real decision once UMP knows it:
     *  - NOT_REQUIRED (no regulation applies, e.g. India)  -> everything granted.
     *  - OBTAINED and GDPR applies -> mapped from the user's TCF purpose choices:
     *      analytics_storage, ad_storage <- Purpose 1 (store / access information on a device)
     *      ad_user_data                 <- Purposes 1 + 7 (measure ad performance)
     *      ad_personalization           <- Purposes 3 + 4 (personalised ads profile + selection)
     *  - OBTAINED, GDPR doesn't apply -> granted.
     *  - REQUIRED / UNKNOWN (form not answered yet, or failed to load) -> left as is (denied).
     * Firebase persists the last setConsent() call, so returning users keep their choice.
     *
     * Not covered: US-state opt-outs ("do not sell/share") are applied to ads by the Mobile Ads
     * SDK itself; Analytics here only follows the GDPR (TCF) choices.
     */
    fun applyAnalyticsConsent(context: Context) {
        val status = UserMessagingPlatform.getConsentInformation(context).consentStatus
        val granted: Map<FirebaseAnalytics.ConsentType, Boolean> = when (status) {
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> allConsentTypes(true)
            ConsentInformation.ConsentStatus.OBTAINED -> fromTcf(context)
            else -> {
                Log.d(TAG, "Analytics consent unchanged (status=$status)")
                return
            }
        }
        FirebaseAnalytics.getInstance(context).setConsent(
            granted.mapValues { (_, ok) ->
                if (ok) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
            }
        )
        Log.d(TAG, "Analytics consent applied: $granted")
    }

    private fun allConsentTypes(value: Boolean) = mapOf(
        FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to value,
        FirebaseAnalytics.ConsentType.AD_STORAGE to value,
        FirebaseAnalytics.ConsentType.AD_USER_DATA to value,
        FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to value
    )

    /** Reads the IAB TCF v2 strings UMP stores in the default SharedPreferences (<package>_preferences). */
    private fun fromTcf(context: Context): Map<FirebaseAnalytics.ConsentType, Boolean> {
        val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        if (prefs.getInt("IABTCF_gdprApplies", 0) != 1) return allConsentTypes(true)
        // "1" / "0" per purpose, purpose N at index N-1.
        val purposes = prefs.getString("IABTCF_PurposeConsents", "").orEmpty()
        fun purpose(n: Int) = purposes.getOrNull(n - 1) == '1'
        return mapOf(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to purpose(1),
            FirebaseAnalytics.ConsentType.AD_STORAGE to purpose(1),
            FirebaseAnalytics.ConsentType.AD_USER_DATA to (purpose(1) && purpose(7)),
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to (purpose(3) && purpose(4))
        )
    }

    /** Reset consent — useful for testing. Call from debug menu only. */
    fun resetForTesting(context: Context) {
        if (BuildConfig.DEBUG) {
            UserMessagingPlatform.getConsentInformation(context).reset()
            Log.d(TAG, "Consent reset for testing")
        }
    }
}

