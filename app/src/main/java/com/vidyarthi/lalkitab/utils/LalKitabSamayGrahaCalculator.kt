package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.KundliData
import swisseph.DblObj
import swisseph.SweConst
import java.time.LocalDate
import kotlin.math.min

/**
 * Lal Kitab janma samay graha (daytime):
 * - Sunrise to sunset split into 11 equal bhag (no empty remainder).
 * - Guru 2, Surya 2, Chandra 1, Mangal 2, Shukra 2, Budh 2.
 * - Night: Rahu till evening civil twilight (-6 deg), last 2 kalak before sunrise = Ketu,
 *   middle night Chandra after moonrise, else Amavasya => Shukra, otherwise Shani.
 */
object LalKitabSamayGrahaCalculator {

    private val DAY_BHAG_PLANETS = listOf(
        "ગુરુ" to 2,
        "સૂર્ય" to 2,
        "ચંદ્ર" to 1,
        "મંગળ" to 2,
        "શુક્ર" to 2,
        "બુધ" to 2
    )

    private const val TOTAL_DAY_BHAG = 11

    fun planetKeyAtBirth(k: KundliData): String {
        val birthJD = TimeUtils.julianDayUtcForBirth(k)
        val dayInfo = PanchangEngine.calculateDayInfo(k)
        return planetKeyAtBirthJd(k, birthJD, dayInfo)
    }

    fun planetKeyAtBirthJd(k: KundliData, birthJD: Double, dayInfo: com.vidyarthi.lalkitab.data.PanchangDayInfo): String {
        val sunriseJD = dayInfo.sunriseJD
        val sunsetJD = dayInfo.sunsetJD
        val nextSunriseJD = dayInfo.nextSunriseJD
        val birthDate = LocalDate.of(k.year, k.month, k.day)
        val prevDate = birthDate.minusDays(1)

        var prevSunsetJD = PanchangEngine.sunRiseSetSwiss(
            prevDate.year, prevDate.monthValue, prevDate.dayOfMonth,
            k.longitude, k.latitude, k.timezone, false
        )
        while (prevSunsetJD >= sunriseJD) prevSunsetJD -= 1.0
        while (prevSunsetJD < sunriseJD - 1.25) prevSunsetJD += 1.0

        val moonriseJD = PanchangEngine.moonRiseJdUt(
            k.year, k.month, k.day,
            k.longitude, k.latitude, k.timezone
        )?.let { normalizeMoonToSunriseDay(it, sunriseJD) }
        val moonrisePrevDayJD = PanchangEngine.moonRiseJdUt(
            prevDate.year, prevDate.monthValue, prevDate.dayOfMonth,
            k.longitude, k.latitude, k.timezone
        )?.let { normalizeMoonToSunriseDay(it, sunriseJD) }
        val tithiInfo = PanchangEngine.calculateTithiInfo(k)
        val isAmavasyaTithi = tithiInfo.janmaTithiIndex == 30

        val bhagLen = (sunsetJD - sunriseJD) / TOTAL_DAY_BHAG
        val nightKalakLenThis = (nextSunriseJD - sunsetJD) / 12.0
        val nightKalakLenPrev = (sunriseJD - prevSunsetJD) / 12.0

        if (bhagLen <= 1e-8 || nightKalakLenThis <= 1e-8 || nightKalakLenPrev <= 1e-8) {
            return "શનિ"
        }

        if (birthJD >= sunriseJD && birthJD < sunsetJD) {
            val bhagIndex = (birthJD - sunriseJD) / bhagLen
            return dayLordFromBhagIndex(bhagIndex)
        }

        if (birthJD >= sunsetJD && birthJD < nextSunriseJD) {
            val civilDuskEnd = eveningCivilTwilightEndJD(
                sunsetJD, nextSunriseJD, k.longitude, k.latitude
            ) ?: (sunsetJD + min(2.0 * nightKalakLenThis, 2.5 / 24.0))
            if (birthJD < civilDuskEnd) return "રાહુ"
            val h = (birthJD - sunsetJD) / nightKalakLenThis
            if (h >= 10.0) return "કેતુ"
            return nightMiddleLord(
                birthJD = birthJD,
                moonriseTodayJD = moonriseJD,
                moonrisePrevDayJD = moonrisePrevDayJD,
                isAmavasyaTithi = isAmavasyaTithi,
                beforeSunrise = false
            )
        }

        if (birthJD < sunriseJD) {
            val civilDuskEndPrev = eveningCivilTwilightEndJD(
                prevSunsetJD, sunriseJD, k.longitude, k.latitude
            ) ?: (prevSunsetJD + min(2.0 * nightKalakLenPrev, 2.5 / 24.0))
            if (birthJD < civilDuskEndPrev) return "રાહુ"
            val h = (birthJD - prevSunsetJD) / nightKalakLenPrev
            if (h >= 10.0) return "કેતુ"
            return nightMiddleLord(
                birthJD = birthJD,
                moonriseTodayJD = moonriseJD,
                moonrisePrevDayJD = moonrisePrevDayJD,
                isAmavasyaTithi = isAmavasyaTithi,
                beforeSunrise = true
            )
        }

        return "શનિ"
    }

    private fun normalizeMoonToSunriseDay(jdEvent: Double, sunriseJdUT: Double): Double {
        var jd = jdEvent
        if (jd < sunriseJdUT - 0.5) jd += 1.0
        return jd
    }

    private fun dayLordFromBhagIndex(bhagIndex: Double): String {
        var cum = 0.0
        for ((name, w) in DAY_BHAG_PLANETS) {
            cum += w.toDouble()
            if (bhagIndex < cum) return name
        }
        return DAY_BHAG_PLANETS.last().first
    }

    private fun eveningCivilTwilightEndJD(
        sunsetJdUT: Double,
        nightEndJdUT: Double,
        lon: Double,
        lat: Double
    ): Double? {
        val sw = SwissEphManager.get()
        val geopos = doubleArrayOf(lon, lat, 0.0)
        val tret = DblObj()
        val serr = StringBuffer()
        val jdStart = sunsetJdUT + 1.0 / 1440.0
        val result = sw.swe_rise_trans_true_hor(
            jdStart,
            SweConst.SE_SUN,
            StringBuffer(),
            SweConst.SEFLG_SWIEPH,
            SweConst.SE_CALC_SET,
            geopos,
            1013.25,
            15.0,
            -6.0,
            tret,
            serr
        )
        if (result < 0) return null
        val t = tret.`val`
        if (t <= sunsetJdUT || t >= nightEndJdUT) return null
        return t
    }

    private fun nightMiddleLord(
        birthJD: Double,
        moonriseTodayJD: Double?,
        moonrisePrevDayJD: Double?,
        isAmavasyaTithi: Boolean,
        beforeSunrise: Boolean
    ): String {
        if (beforeSunrise) {
            if (moonrisePrevDayJD != null && birthJD >= moonrisePrevDayJD - 1e-8) return "ચંદ્ર"
        } else {
            if (moonriseTodayJD != null && birthJD >= moonriseTodayJD - 1e-8) return "ચંદ્ર"
        }
        if (isAmavasyaTithi) return "શુક્ર"
        return "શનિ"
    }
}
