package com.vidyarthi.lalkitab.data

data class CityInfo(
    val city: String,
    val state: String? = null,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: Double,
    val timezoneId: String? = null,
)
