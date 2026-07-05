package com.vidyarthi.lalkitab.data

data class PanchangDayInfo(
    val weekday: String,
    val sunrise: String,
    val sunset: String,
    val sunriseJD: Double,

    val sunsetJD: Double,

    /** પાછલા દિવસનો સૂર્યાસ્ત (આજના સૂર્યોદય પહેલાંની રાત માટે). */
    val prevSunsetJD: Double,

    val nextSunriseJD: Double,
    val moonrise: String?,
    val moonset: String?,

    val moonriseJD: Double?,

    /** પાછલા દિવસે ચંદ્રોદય (સૂર્યોદય પહેલાં જન્મ માટે ચંદ્ર રાત). */
    val moonrisePrevDayJD: Double?,
    val dayLength: String,
    val nightLength: String
)
