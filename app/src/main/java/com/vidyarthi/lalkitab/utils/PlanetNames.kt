package com.vidyarthi.lalkitab.utils

import android.content.Context
import com.vidyarthi.lalkitab.R

/**
 * [KundliEngine] and dasha logic use Gujarati strings as stable internal keys.
 * Use these helpers for user-visible labels (English / Hindi per app locale).
 */
object PlanetNames {

    private val nameToFullRes: Map<String, Int> = mapOf(
        "સૂર્ય" to R.string.planet_surya,
        "ચંદ્ર" to R.string.planet_chandra,
        "મંગળ" to R.string.planet_mangal,
        "બુધ" to R.string.planet_budh,
        "ગુરુ" to R.string.planet_guru,
        "શુક્ર" to R.string.planet_shukra,
        "શનિ" to R.string.planet_shani,
        "રાહુ" to R.string.planet_rahu,
        "કેતુ" to R.string.planet_ketu
    )

    private val nameToShortRes: Map<String, Int> = mapOf(
        "સૂર્ય" to R.string.planet_abbr_surya,
        "ચંદ્ર" to R.string.planet_abbr_chandra,
        "મંગળ" to R.string.planet_abbr_mangal,
        "બુધ" to R.string.planet_abbr_budh,
        "ગુરુ" to R.string.planet_abbr_guru,
        "શુક્ર" to R.string.planet_abbr_shukra,
        "શનિ" to R.string.planet_abbr_shani,
        "રાહુ" to R.string.planet_abbr_rahu,
        "કેતુ" to R.string.planet_abbr_ketu
    )

    fun localizedName(context: Context, gujaratiName: String): String {
        val id = nameToFullRes[gujaratiName.trim()] ?: return gujaratiName
        return context.getString(id)
    }

    fun shortName(context: Context, gujaratiName: String): String {
        val id = nameToShortRes[gujaratiName.trim()] ?: return gujaratiName.take(2)
        return context.getString(id)
    }
}
