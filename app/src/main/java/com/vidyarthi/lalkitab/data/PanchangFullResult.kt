package com.vidyarthi.lalkitab.data

data class PanchangFullResult(
    val dayInfo: PanchangDayInfo,
    val tithiInfo: PanchangTithiInfo,
    val nakshatraInfo: PanchangNakshatraInfo,
    val yogaKaranaInfo: PanchangYogaKaranaInfo,
    val monthInfo: PanchangMonthInfo,
    val horaInfo: String?
)
