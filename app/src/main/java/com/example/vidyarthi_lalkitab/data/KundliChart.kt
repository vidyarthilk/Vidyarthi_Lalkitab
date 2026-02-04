package com.example.vidyarthi_lalkitab.data

data class KundliChart(
    val lagnaRashi: String,
    val lagnaDegree: Double,
    val houses: List<Double>,          // 12 bhava cusps
    val grahas: List<GrahaPosition>
)
