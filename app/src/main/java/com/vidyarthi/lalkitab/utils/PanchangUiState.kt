package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.*

data class PanchangUiState(
    val isLoading: Boolean = false,

    val dayInfo: PanchangDayInfo? = null,
    val tithiInfo: PanchangTithiInfo? = null,
    val nakshatraInfo: PanchangNakshatraInfo? = null,
    val yogaKaranaInfo: PanchangYogaKaranaInfo? = null,

    val monthInfo: PanchangMonthInfo? = null,
    val pakshaInfo: PanchangPakshaInfo? = null,
    val vikramSamvat: Int? = null,

    /** Internal graha keys ([PlanetNames]); weekday lord for birth date. */
    val birthdayPlanetKey: String? = null,
    /** Lal Kitab janma samay graha (kalak = 60 min; not Vedic hora). */
    val birthtimePlanetKey: String? = null,

    /** Same as [birthtimePlanetKey] for 35-yr dasha ([KundliHolder]); redundant field for UI clarity. */
    val samayGrahaDashaName: String? = null,
    val samayGrahaJanmaHouse: Int? = null,
    val error: String? = null
) {

    val hasData: Boolean
        get() = dayInfo != null && tithiInfo != null && nakshatraInfo != null
}