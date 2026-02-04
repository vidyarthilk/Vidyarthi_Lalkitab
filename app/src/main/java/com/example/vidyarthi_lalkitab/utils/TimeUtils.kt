package com.example.vidyarthi_lalkitab.utils

object TimeUtils {

    fun localToUT(
        hour: Int,
        minute: Int,
        timezone: Double
    ): Double {
        return hour + minute / 60.0 - timezone
    }
}
