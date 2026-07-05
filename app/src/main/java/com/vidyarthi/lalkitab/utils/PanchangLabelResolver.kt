package com.vidyarthi.lalkitab.utils

import android.content.Context
import com.vidyarthi.lalkitab.R

/** Localized Panchang names from string-array resources (English / Hindi). */
object PanchangLabelResolver {

    fun tithi(context: Context, index1Based: Int): String {
        if (index1Based !in 1..30) return ""
        val arr = context.resources.getStringArray(R.array.panchang_tithi)
        return arr.getOrNull(index1Based - 1) ?: ""
    }

    fun nakshatra(context: Context, index0Based: Int): String {
        val i = index0Based.coerceIn(0, 26)
        val arr = context.resources.getStringArray(R.array.panchang_nakshatra)
        return arr.getOrNull(i) ?: ""
    }

    fun yoga(context: Context, index1Based: Int): String {
        val i = index1Based.coerceIn(1, 27) - 1
        val arr = context.resources.getStringArray(R.array.panchang_yoga)
        return arr.getOrNull(i) ?: ""
    }

    /** [karanaIndex] is 1-based as stored in [com.vidyarthi.lalkitab.data.PanchangYogaKaranaInfo]. */
    fun karana(context: Context, karanaIndex1Based: Int): String {
        val i = (karanaIndex1Based - 1).coerceIn(0, 10)
        val arr = context.resources.getStringArray(R.array.panchang_karana)
        return arr.getOrNull(i) ?: ""
    }

    fun maas(context: Context, index0Based: Int): String {
        val i = index0Based.coerceIn(0, 11)
        val arr = context.resources.getStringArray(R.array.panchang_maas)
        return arr.getOrNull(i) ?: ""
    }

    fun paksha(context: Context, key: String): String {
        return when (key) {
            "Shukla" -> context.getString(R.string.paksha_shukla)
            "Krishna" -> context.getString(R.string.paksha_krishna)
            else -> key
        }
    }
}
