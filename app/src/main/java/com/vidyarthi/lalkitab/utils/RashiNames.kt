package com.vidyarthi.lalkitab.utils

import android.content.Context
import com.vidyarthi.lalkitab.R

/** Sidereal sign labels; [KundliEngine] stores Gujarati names as stable keys. */
object RashiNames {

    private val gujaratiToRes: Map<String, Int> = mapOf(
        "મેષ" to R.string.rashi_mesha,
        "વૃષભ" to R.string.rashi_vrishabh,
        "મિથુન" to R.string.rashi_mithun,
        "કર્ક" to R.string.rashi_kark,
        "સિંહ" to R.string.rashi_singh,
        "કન્યા" to R.string.rashi_kanya,
        "તુલા" to R.string.rashi_tula,
        "વૃશ્ચિક" to R.string.rashi_vrishchik,
        "ધનુ" to R.string.rashi_dhanu,
        "મકર" to R.string.rashi_makar,
        "કુંભ" to R.string.rashi_kumbh,
        "મીન" to R.string.rashi_meena
    )

    fun localizedName(context: Context, gujaratiRashi: String): String {
        val id = gujaratiToRes[gujaratiRashi.trim()] ?: return gujaratiRashi
        return context.getString(id)
    }
}
