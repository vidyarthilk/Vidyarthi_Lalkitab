package com.vidyarthi.lalkitab.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** Google UMP — EU/UK/EEA ad consent before AdMob loads. */
object ConsentManager {

    @Volatile
    private var gatheringComplete = false

    /** True after the consent flow finished (form shown or not required). */
    fun isGatheringComplete(): Boolean = gatheringComplete

    /** AdMob may load ads only when this is true (required in EEA/UK). */
    fun canRequestAds(context: Context): Boolean {
        if (!gatheringComplete) return false
        return try {
            UserMessagingPlatform.getConsentInformation(context.applicationContext)
                .canRequestAds()
        } catch (_: Exception) {
            false
        }
    }

    fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        gatheringComplete = false
        try {
            val consent = UserMessagingPlatform.getConsentInformation(activity)
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()
            consent.requestConsentInfoUpdate(
                activity,
                params,
                {
                    try {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                            gatheringComplete = true
                            onComplete()
                        }
                    } catch (_: Exception) {
                        gatheringComplete = true
                        onComplete()
                    }
                },
                {
                    gatheringComplete = true
                    onComplete()
                }
            )
        } catch (_: Exception) {
            gatheringComplete = true
            onComplete()
        }
    }

    fun showPrivacyOptions(activity: Activity, onDismiss: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDismiss() }
    }

    fun isPrivacyOptionsRequired(activity: Activity): Boolean {
        return UserMessagingPlatform.getConsentInformation(activity)
            .privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }
}
