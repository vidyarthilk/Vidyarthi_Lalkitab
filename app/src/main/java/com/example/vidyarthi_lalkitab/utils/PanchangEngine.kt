package com.example.vidyarthi_lalkitab.utils

import com.example.vidyarthi_lalkitab.data.KundliData
import com.example.vidyarthi_lalkitab.data.PanchangResult
import swisseph.SweConst
import swisseph.SweDate
import java.util.*

object PanchangEngine {

    fun calculate(k: KundliData): PanchangResult {

        // ── Time → UT ──
        val utTime = TimeUtils.localToUT(
            k.hour, k.minute, k.timezone
        )

        val jd = SweDate(
            k.year,
            k.month,
            k.day,
            utTime,
            SweDate.SE_GREG_CAL
        ).julDay

        val sun = DoubleArray(6)
        val moon = DoubleArray(6)

        SwissEphManager.sw.swe_calc(
            jd, SweConst.SE_SUN,
            SweConst.SEFLG_SIDEREAL, sun, null
        )
        SwissEphManager.sw.swe_calc(
            jd, SweConst.SE_MOON,
            SweConst.SEFLG_SIDEREAL, moon, null
        )

        val sunLon = sun[0]
        val moonLon = moon[0]

        // ── Vara ──
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(k.year, k.month - 1, k.day)
        val vara = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "રવિવાર"
            Calendar.MONDAY -> "સોમવાર"
            Calendar.TUESDAY -> "મંગળવાર"
            Calendar.WEDNESDAY -> "બુધવાર"
            Calendar.THURSDAY -> "ગુરુવાર"
            Calendar.FRIDAY -> "શુક્રવાર"
            else -> "શનિવાર"
        }

        // ── Tithi ──
        val diff = (moonLon - sunLon + 360) % 360
        val tithiIndex = (diff / 12).toInt()

        // ── Nakshatra + Charan ──
        val nakIndex = (moonLon / 13.333333).toInt()
        val charan = ((moonLon % 13.333333) / 3.333333).toInt() + 1

        // ── Yoga ──
        val yogaIndex = (((sunLon + moonLon) % 360) / 13.333333).toInt()

        return PanchangResult(
            vara = vara,
            tithi = PanchangConstants.TITHI[tithiIndex],
            nakshatra = PanchangConstants.NAKSHATRA[nakIndex],
            charan = charan,
            yoga = PanchangConstants.YOGA[yogaIndex]
        )
    }
}
