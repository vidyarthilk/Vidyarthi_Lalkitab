package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.KundliData

object KundliHolder {
    var kundliData: KundliData? = null

    /** City / gender for PDF birth-details page (session only). */
    var sessionCity: String? = null
    var sessionGender: String? = null

    /**
     * Janma samay graha for Lal Kitab 35-year dasha (internal key; same as [KundliEngine] graha name).
     * Panchang load resolves house from janma kundli; dasha screen uses this name.
     */
    var dashaSamayGrahaName: String = "ચંદ્ર"
}