package com.vidyarthi.lalkitab.data

import com.vidyarthi.lalkitab.utils.IndiaStateInfer

/**
 * One row from the cities DB for autocomplete / save.
 * [toString] must match [CityRepository.parseDisplayLine] so saved kundli lines resolve again.
 */
data class CityPick(
    val city: String,
    val state: String?,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String?,
    val dst: Int,
) {
    /** ડ્રોપડાઉન / સેવ માટે: DB માં રાજ્ય ન હોય તો ભારત માટે લાટ/લોનથી અંદાજ. */
    fun resolvedStateForDisplay(): String? {
        val s = state?.trim().orEmpty()
        if (s.isNotEmpty()) return s
        if (country.equals("India", ignoreCase = true)) {
            return IndiaStateInfer.infer(latitude, longitude)
        }
        return null
    }

    override fun toString(): String {
        val st = resolvedStateForDisplay()?.trim().orEmpty()
        return if (st.isNotEmpty()) "$city, $st, $country" else "$city, $country"
    }
}
