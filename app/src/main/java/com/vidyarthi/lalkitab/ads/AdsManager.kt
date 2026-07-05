package com.vidyarthi.lalkitab.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.vidyarthi.lalkitab.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Google AdMob — free users see ads. Replace test unit IDs in strings.xml with your AdMob IDs before release.
 */
object AdsManager {

    private const val PREFS = "ads_prefs"
    private const val KEY_LAST_INTERSTITIAL_MS = "last_interstitial_ms"
    private const val INTERSTITIAL_COOLDOWN_MS = 3 * 60 * 1000L

    @Volatile
    private var initialized = false

    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            MobileAds.initialize(context.applicationContext) {}
            initialized = true
        }
    }

    fun areAdsEnabled(context: Context): Boolean {
        if (com.vidyarthi.lalkitab.subscription.SubscriptionManager.isSubscribed(context)) {
            return false
        }
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("ads_enabled", true)
    }

    fun loadBanner(adView: AdView) {
        try {
            adView.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            adView.visibility = android.view.View.GONE
        }
    }

    fun bindBanner(activity: Activity, adView: AdView?) {
        if (adView == null || !areAdsEnabled(activity) || !ConsentManager.canRequestAds(activity)) {
            adView?.visibility = android.view.View.GONE
            return
        }
        if (!initialized) {
            adView?.visibility = android.view.View.GONE
            return
        }
        try {
            // adUnitId is already set in layout_ad_banner.xml — do not set twice (crashes).
            if (adView.adUnitId.isNullOrBlank()) {
                adView.adUnitId = activity.getString(R.string.admob_banner_main)
            }
            loadBanner(adView)
        } catch (e: Exception) {
            adView.visibility = android.view.View.GONE
        }
    }

    fun preloadInterstitial(context: Context) {
        if (!areAdsEnabled(context) || !ConsentManager.canRequestAds(context) || !initialized) return
        if (interstitialAd != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            context,
            context.getString(R.string.admob_interstitial_main),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    interstitialLoading = false
                }
            }
        )
    }

    /**
     * Shows a full-screen ad when opening kundli flow, at most once per [INTERSTITIAL_COOLDOWN_MS].
     */
    fun showInterstitialIfReady(activity: Activity, onFinished: () -> Unit = {}) {
        if (!areAdsEnabled(activity) || !ConsentManager.canRequestAds(activity) || !initialized) {
            onFinished()
            return
        }
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_INTERSTITIAL_MS, 0L)
        val ad = interstitialAd
        if (ad == null || now - last < INTERSTITIAL_COOLDOWN_MS) {
            preloadInterstitial(activity)
            onFinished()
            return
        }
        ad.fullScreenContentCallback =
            object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onFinished()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onFinished()
                }
            }
        prefs.edit().putLong(KEY_LAST_INTERSTITIAL_MS, now).apply()
        interstitialAd = null
        ad.show(activity)
    }

    fun pauseBanner(adView: AdView?) {
        adView?.pause()
    }

    fun resumeBanner(adView: AdView?) {
        adView?.resume()
    }

    fun destroyBanner(adView: AdView?) {
        adView?.destroy()
    }
}
