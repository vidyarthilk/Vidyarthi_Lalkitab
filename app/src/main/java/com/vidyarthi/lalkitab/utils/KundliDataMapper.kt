package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.CityPick
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.data.entity.KundliEntity

object KundliDataMapper {

    fun fromCityPick(
        name: String,
        date: String,
        time: String,
        city: CityPick
    ): KundliData? {
        val parsed = parseDateTime(date, time) ?: return null
        val (year, month, day, hour, minute) = parsed
        val tzId = city.timezoneId?.trim().takeIf { !it.isNullOrEmpty() }
        val tz = if (tzId != null) {
            TimeUtils.timezoneOffsetForLocalDateTime(tzId, year, month, day, hour, minute)
        } else {
            5.5
        }
        return KundliData(
            name = name.trim(),
            year = year,
            month = month,
            day = day,
            hour = hour,
            minute = minute,
            latitude = city.latitude,
            longitude = city.longitude,
            timezone = tz,
            timezoneId = tzId
        )
    }

    fun fromEntity(entity: KundliEntity, city: CityPick): KundliData? =
        fromCityPick(entity.name, entity.date, entity.time, city)

    private fun parseDateTime(date: String, time: String): Quintuple? {
        val dateParts = date.trim().split("/")
        if (dateParts.size != 3) return null
        val day = dateParts[0].toIntOrNull() ?: return null
        val month = dateParts[1].toIntOrNull() ?: return null
        val year = dateParts[2].toIntOrNull() ?: return null
        val (hour, minute) = TimeInputFormatter.parseTo24Hour(time) ?: return null
        return Quintuple(year, month, day, hour, minute)
    }

    private data class Quintuple(
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int
    )
}
