package com.vidyarthi.lalkitab.data

data class PanchangNakshatraInfo(
    /** 0 … 26; use with [com.vidyarthi.lalkitab.utils.PanchangLabelResolver.nakshatra]. */
    val nakshatraIndex: Int,
    val pada: Int,
    val endTime: String
)