package com.vidyarthi.lalkitab.data

import java.io.Serializable

data class KundliData(
    val name: String,
    val year: Int,
    val month: Int,     // 1–12
    val day: Int,
    val hour: Int,
    val minute: Int,
    val latitude: Double,
    val longitude: Double,
    val timezone: Double,
    /** IANA ઝોન (ઉદા. Asia/Kolkata); null હોય તો ફક્ત [timezone] ઘટક વાપરાય છે. */
    val timezoneId: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
