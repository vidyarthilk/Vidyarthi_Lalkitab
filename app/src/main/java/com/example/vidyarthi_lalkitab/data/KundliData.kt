package com.example.vidyarthi_lalkitab.data

data class KundliData(
    val name: String,
    val year: Int,
    val month: Int,     // 1–12
    val day: Int,
    val hour: Int,
    val minute: Int,
    val latitude: Double,
    val longitude: Double,
    val timezone: Double   // e.g. India = +5.5
)
