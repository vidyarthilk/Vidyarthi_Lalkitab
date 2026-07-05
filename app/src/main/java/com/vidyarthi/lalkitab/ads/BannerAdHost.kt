package com.vidyarthi.lalkitab.ads

import android.view.View
import androidx.fragment.app.Fragment
import com.vidyarthi.lalkitab.R
import com.google.android.gms.ads.AdView

/** Banner ad lifecycle for fragments that only show input / list (no kundli charts). */
class BannerAdHost(private val fragment: Fragment) {

    private var adView: AdView? = null
    private var adsBound = false

    fun attach(root: View) {
        adView = root.findViewById(R.id.adView)
    }

    fun onResume() {
        val view = adView ?: return
        if (!fragment.isAdded) return
        try {
            if (!adsBound) {
                AdsManager.bindBanner(fragment.requireActivity(), view)
                adsBound = true
            }
            AdsManager.resumeBanner(view)
        } catch (_: Exception) {
            view.visibility = View.GONE
            adsBound = false
        }
    }

    fun onPause() {
        AdsManager.pauseBanner(adView)
    }

    fun onDestroyView() {
        AdsManager.destroyBanner(adView)
        adView = null
        adsBound = false
    }
}
