package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.KundliData
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToLong
import swisseph.SweDate

object TimeUtils {

    fun localToUT(hour: Int, minute: Int, timezone: Double): Double {
        val localTime = hour + (minute / 60.0)
        return (localTime + 24 - timezone).mod(24.0)
    }

    fun localToUtcDateTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        timezone: Double
    ): LocalDateTime {
        val offsetSeconds = (timezone * 3600.0).roundToLong()
        return LocalDateTime
            .of(year, month, day, hour, minute)
            .minusSeconds(offsetSeconds)
    }

    fun timezoneOffsetForLocalDateTime(
        timezoneId: String,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Double {
        val zone = ZoneId.of(timezoneId)
        val localDateTime = LocalDateTime.of(year, month, day, hour, minute)
        return localDateTime.atZone(zone).offset.totalSeconds / 3600.0
    }

    fun julianDayFromLocal(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        timezone: Double
    ): Double {
        val utcDateTime = localToUtcDateTime(year, month, day, hour, minute, timezone)
        val utcHours = utcDateTime.hour +
            (utcDateTime.minute / 60.0) +
            (utcDateTime.second / 3600.0) +
            (utcDateTime.nano / 3_600_000_000_000.0)

        return SweDate(
            utcDateTime.year,
            utcDateTime.monthValue,
            utcDateTime.dayOfMonth,
            utcHours,
            SweDate.SE_GREG_CAL
        ).julDay
    }

    fun julianDayUtcForBirth(k: KundliData): Double =
        julianDayUtcForBirth(
            k.year,
            k.month,
            k.day,
            k.hour,
            k.minute,
            k.timezone,
            k.timezoneId
        )

    fun julianDayUtcForBirth(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        timezoneHours: Double,
        timezoneId: String?
    ): Double {
        if (!timezoneId.isNullOrBlank()) {
            try {
                val zone = ZoneId.of(timezoneId)
                val local = LocalDateTime.of(year, month, day, hour, minute)
                val utc = local.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
                val utcHours = utc.hour +
                    utc.minute / 60.0 +
                    utc.second / 3600.0 +
                    utc.nano / 3_600_000_000_000.0
                return SweDate(
                    utc.year,
                    utc.monthValue,
                    utc.dayOfMonth,
                    utcHours,
                    SweDate.SE_GREG_CAL
                ).julDay
            } catch (_: Exception) {
                // નીચે ફિક્સ્ડ ઓફસેટ
            }
        }
        return julianDayFromLocal(year, month, day, hour, minute, timezoneHours)
    }
}
