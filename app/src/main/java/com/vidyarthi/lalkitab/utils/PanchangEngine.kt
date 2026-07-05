package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.*
import swisseph.*
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import swisseph.SweDate
import swisseph.SwissEph
import java.time.DayOfWeek
import java.time.LocalDate

object PanchangEngine {
    private val sw by lazy {
        SwissEphManager.get()  // ✅ Lazy init - safe
    }

    init {
        sw.swe_set_sid_mode(
            SweConst.SE_SIDM_LAHIRI,
            0.0,
            0.0
        )
    }
    /**
     * Amanta lunar month name is decided from Sun's sidereal rashi at the Amavasya instant.
     *
     * IMPORTANT: Our rashi index is based on sidereal longitude where:
     * 0 = Mesh (Aries) ... 11 = Meena (Pisces)
     *
     * Standard mapping (North/Drik; matches common JHora settings):
     * Meena→Chaitra, Mesh→Vaishakh, Vrishabh→Jeth, Mithun→Ashadh,
     * Kark→Shravan, Singh→Bhadravo, Kanya→Aso, Tula→Kartak,
     * Vrishchik→Magshar, Dhanu→Posh, Makar→Maha, Kumbh→Fagan.
     *
     * Indices: maas 0=Chaitra ... 11=Fagan (see PanchangConstants.MAAS)
     */
    private val RASHI_TO_GUJ_MAAS = intArrayOf(
        0,  // Mesh → Chaitra
        1,  // Vrishabh → Vaisakha
        2,  // Mithun → Jyeshtha
        3,  // Kark → Ashadha
        4,  // Singh → Shravana
        5,  // Kanya → Bhadrapada
        6,  // Tula → Ashvina
        7,  // Vrishchik → Kartika
        8,  // Dhanu → Margashirsha
        9,  // Makar → Pausha
        10, // Kumbh → Magha
        11  // Meena → Phalguna
    )
    /* ===================== TIME HELPERS ===================== */
    fun jdToLocalTime(jdUt: Double, tzHours: Double): String {

        val jdLocal = jdUt + tzHours / 24.0

        // JD day starts at noon → shift by 0.5
        val frac = (jdLocal + 0.5) % 1.0
        val totalMinutes = (frac * 1440).toInt()

        val hour = totalMinutes / 60
        val minute = totalMinutes % 60

        return "%02d:%02d".format(hour, minute)
    }
    /* ===================== DAY INFO ===================== */
    fun calculateDayInfo(k: KundliData): PanchangDayInfo {

        val sunriseJdUT = sunRiseSetSwiss(
            k.year, k.month, k.day,
            k.longitude, k.latitude,
            k.timezone,
            true
        )

        val sunsetJdUT = sunRiseSetSwiss(
            k.year, k.month, k.day,
            k.longitude, k.latitude,
            k.timezone,
            false
        )

        var adjSunriseUT = sunriseJdUT
        var adjSunsetUT = sunsetJdUT

        if (adjSunsetUT < adjSunriseUT) {
            adjSunsetUT += 1.0
        }

        // Hindu panchang day: sunrise to next sunrise (not civil midnight).
        val birthJD = TimeUtils.julianDayUtcForBirth(k)
        val weekdayGuj = panchangWeekdayGuj(k, birthJD, adjSunriseUT)

        val birthDate = LocalDate.of(k.year, k.month, k.day)
        val prevDate = birthDate.minusDays(1)
        val nextDate = birthDate.plusDays(1)
        var prevSunsetJD = sunRiseSetSwiss(
            prevDate.year, prevDate.monthValue, prevDate.dayOfMonth,
            k.longitude, k.latitude, k.timezone, false
        )
        while (prevSunsetJD >= adjSunriseUT) prevSunsetJD -= 1.0
        while (prevSunsetJD < adjSunriseUT - 1.25) prevSunsetJD += 1.0
        val moonriseJDRaw = moonRiseJdUt(
            k.year, k.month, k.day,
            k.longitude, k.latitude, k.timezone
        )
        val moonriseJD = moonriseJDRaw?.let { normalizeMoonToSunriseDay(it, adjSunriseUT) }
        val moonrisePrevJDRaw = moonRiseJdUt(
            prevDate.year, prevDate.monthValue, prevDate.dayOfMonth,
            k.longitude, k.latitude, k.timezone
        )
        val moonrisePrevDayJD = moonrisePrevJDRaw?.let { normalizeMoonToSunriseDay(it, adjSunriseUT) }
        val moonriseUT = moonRiseSetSwiss(
            k.year, k.month, k.day,
            k.longitude, k.latitude,
            k.timezone,
            true
        )

        val moonsetUT = moonRiseSetSwiss(
            k.year, k.month, k.day,
            k.longitude, k.latitude,
            k.timezone,
            false
        )
        val moonriseLocal = moonriseUT?.let {
            val jdNorm = normalizeMoonToSunriseDay(it, adjSunriseUT)
            jdToLocalTime(jdNorm, k.timezone)
        }

        val moonsetLocal = moonsetUT?.let {
            val jdNorm = normalizeMoonToSunriseDay(it, adjSunriseUT)
            jdToLocalTime(jdNorm, k.timezone)
        }

        val dayMinutes = ((adjSunsetUT - adjSunriseUT) * 1440).roundToInt()

        var nextSunriseJD = sunRiseSetSwiss(
            nextDate.year, nextDate.monthValue, nextDate.dayOfMonth,
            k.longitude, k.latitude, k.timezone, true
        )
        while (nextSunriseJD <= adjSunsetUT) nextSunriseJD += 1.0

        val nightMinutes = ((nextSunriseJD - adjSunsetUT) * 1440).roundToInt()
        return PanchangDayInfo(
            weekday = weekdayGuj,
            sunrise = jdToLocalTime(adjSunriseUT, k.timezone),
            sunset = jdToLocalTime(adjSunsetUT, k.timezone),
            sunriseJD = adjSunriseUT,
            sunsetJD = adjSunsetUT,
            prevSunsetJD = prevSunsetJD,
            nextSunriseJD = nextSunriseJD,
            moonrise = moonriseLocal,
            moonset = moonsetLocal,
            moonriseJD = moonriseJD,
            moonrisePrevDayJD = moonrisePrevDayJD,
            dayLength = minutesToHHMM(dayMinutes),
            nightLength = minutesToHHMM(nightMinutes)
        )
    }

    private fun gujaratiWeekday(dow: Int): String = when (dow) {
        Calendar.SUNDAY -> "રવિવાર"
        Calendar.MONDAY -> "સોમવાર"
        Calendar.TUESDAY -> "મંગળવાર"
        Calendar.WEDNESDAY -> "બુધવાર"
        Calendar.THURSDAY -> "ગુરુવાર"
        Calendar.FRIDAY -> "શુક્રવાર"
        Calendar.SATURDAY -> "શનિવાર"
        else -> ""
    }

    /* ===================== TITHI ===================== */
    fun calculateTithiInfo(k: KundliData): PanchangTithiInfo {

        val sunriseJD = sunRiseSetSwiss(
            k.year, k.month, k.day,
            k.longitude, k.latitude,
            k.timezone,
            true
        )

        var phase = moonSunDiff(sunriseJD)
        if (phase >= 360.0 - 1e-9) phase = 0.0
        val tithiIndex = tithiIndexFromPhase(phase)
        val tithiEndJd = findNextAngleMultipleUt(
            startJdUt = sunriseJD,
            startAngleDeg = phase,
            segmentDeg = 12.0,
            angleProvider = ::moonSunDiff,
            maxDaysAhead = 2.0
        )
        val birthJD = TimeUtils.julianDayUtcForBirth(k)
        val janmaIndex = tithiIndexAtJd(birthJD)
        return PanchangTithiInfo(
            uditaTithi = PanchangConstants.TITHI_NAMES[tithiIndex - 1],
            uditaTithiIndex = tithiIndex,
            janmaTithi = PanchangConstants.TITHI_NAMES[janmaIndex - 1],
            janmaTithiIndex = janmaIndex,
            endTime = jdToLocalTime(tithiEndJd, k.timezone)
        )
    }

    private fun tithiIndexFromPhase(phase: Double): Int {
        val raw = floor(phase / 12.0).toInt() + 1
        return raw.coerceIn(1, 30)
    }

    private fun tithiIndexAtJd(jd: Double): Int {
        var phase = moonSunDiff(jd)
        if (phase >= 360.0 - 1e-9) phase = 0.0
        return tithiIndexFromPhase(phase)
    }
    /* ===================== NAKSHATRA ===================== */
    fun calculateNakshatraInfo(k: KundliData): PanchangNakshatraInfo {
        // Birth-time based Nakshatra (matches JHora chart-style Panchang)
        val birthJD = TimeUtils.julianDayUtcForBirth(k)

        val moonLon = lunarLongitude(birthJD)
        val nakIndex = floor(moonLon / (360.0 / 27.0)).toInt().coerceIn(0, 26)
        val pada = floor((moonLon % (360.0 / 27.0)) / (360.0 / 108.0)).toInt() + 1
        val nakEndJd = findNextAngleMultipleUt(
            startJdUt = birthJD,
            startAngleDeg = moonLon,
            segmentDeg = 360.0 / 27.0,
            angleProvider = ::lunarLongitude,
            maxDaysAhead = 2.0
        )
        val ayan = sw.swe_get_ayanamsa_ut(birthJD)

        return PanchangNakshatraInfo(
            nakshatraIndex = nakIndex,
            pada = pada,
            endTime = jdToLocalTime(nakEndJd, k.timezone)
        )
    }
    /* ===================== YOGA + KARANA ===================== */
    fun calculateYogaKaranaInfoAtJd(jdUt: Double, timezone: Double? = null): PanchangYogaKaranaInfo {

        // already sidereal → no ayanamsa subtraction
        val lunar = lunarLongitude(jdUt)
        val solar = solarLongitude(jdUt)

        val sum = (lunar + solar) % 360.0
        val yogaIndex = floor(sum / (360.0 / 27.0)).toInt() + 1
        val yogaIdxSafe = yogaIndex.coerceIn(1, 27)

        val phase = lunarPhase(jdUt)

        val tithiIndex = floor((phase + 1e-9) / 12.0).toInt() + 1

        val tithiProgress = phase % 12.0

        val runningHalf = if (tithiProgress < 6.0) 1 else 2

        val halfTithi = (tithiIndex - 1) * 2 + runningHalf

        val karanaIdx = when {
            halfTithi == 1 -> 0
            halfTithi in 2..57 -> ((halfTithi - 2) % 7) + 1
            halfTithi == 58 -> 8
            halfTithi == 59 -> 9
            halfTithi == 60 -> 10
            else -> 0
        }

        val yogaEndJd = findNextAngleMultipleUt(
            startJdUt = jdUt,
            startAngleDeg = sum,
            segmentDeg = 360.0 / 27.0,
            angleProvider = ::yogaAngleDeg,
            maxDaysAhead = 2.0
        )

        val karanaEndJd = findNextAngleMultipleUt(
            startJdUt = jdUt,
            startAngleDeg = phase,
            segmentDeg = 6.0, // karana changes every half-tithi (6 degrees)
            angleProvider = ::lunarPhase,
            maxDaysAhead = 1.5
        )

        return PanchangYogaKaranaInfo(
            yoga = PanchangConstants.YOGA_NAMES[yogaIdxSafe - 1],
            yogaIndex = yogaIdxSafe,
            yogaEndTime = timezone?.let { jdToLocalTime(yogaEndJd, it) } ?: "",
            karana = PanchangConstants.KARANA_NAMES[karanaIdx],
            karanaIndex = karanaIdx + 1,
            karanaEndTime = timezone?.let { jdToLocalTime(karanaEndJd, it) } ?: ""
        )
    }

    fun calculateYogaKaranaInfoAtSunrise(k: KundliData, sunriseJdUt: Double): PanchangYogaKaranaInfo {
        val base = calculateYogaKaranaInfoAtJd(sunriseJdUt, k.timezone)
        val sum = yogaAngleDeg(sunriseJdUt)
        val phase = lunarPhase(sunriseJdUt)
        val sunLon = solarLongitude(sunriseJdUt)
        val moonLon = lunarLongitude(sunriseJdUt)
        val ayan = sw.swe_get_ayanamsa_ut(sunriseJdUt)

        val yogaEndJd = findNextAngleMultipleUt(
            startJdUt = sunriseJdUt,
            startAngleDeg = sum,
            segmentDeg = 360.0 / 27.0,
            angleProvider = ::yogaAngleDeg,
            maxDaysAhead = 2.0
        )

        val karanaEndJd = findNextAngleMultipleUt(
            startJdUt = sunriseJdUt,
            startAngleDeg = phase,
            segmentDeg = 6.0,
            angleProvider = ::lunarPhase,
            maxDaysAhead = 1.5
        )

        val yogaIdx = floor(sum / (360.0 / 27.0)).toInt() + 1

        return base.copy(
            yogaEndTime = jdToLocalTime(yogaEndJd, k.timezone),
            karanaEndTime = jdToLocalTime(karanaEndJd, k.timezone)
        )
    }
    /* ===================== MASA ===================== */
    data class MonthInfo(
        val maas: Int,        // 0 = Chaitra ... 11 = Phalguna
        val isAdhik: Boolean
    )

    private fun conjDiffSignedDeg(jdUt: Double): Double {
        // Signed (Moon - Sun) in [-180, +180]
        var d = moonSunDiff(jdUt)
        if (d > 180.0) d -= 360.0
        return d
    }

    private fun refineConjunctionNewton(jdSeedUt: Double): Double {
        var t = jdSeedUt
        repeat(20) {
            val sun = solarLonSpeed(t)
            val moon = lunarLonSpeed(t)

            var diff = (moon.lon - sun.lon) % 360.0
            if (diff < 0) diff += 360.0
            if (diff > 180.0) diff -= 360.0 // [-180, +180]

            val relSpeed = (moon.speed - sun.speed).coerceAtLeast(1e-6)
            val dt = diff / relSpeed
            t -= dt
            if (abs(diff) < 1e-7) return t
        }
        return t
    }

    private fun lastAmavasyaBefore(jdUt: Double): Double {
        // Step back ~half a lunar month, refine to conjunction, repeat if still on/after jdUt.
        // Coarse 0.5-day scanning can miss the latest Amavasya and pick one month too early.
        var ama = refineConjunctionNewton(jdUt - 15.0)
        while (ama >= jdUt) {
            ama = refineConjunctionNewton(ama - 29.5306)
        }
        return ama
    }

    /** Local calendar date of the Amavasya that started the current lunar month (before [jdSunriseUt]). */
    fun amavasyaLocalDateBeforeSunrise(jdSunriseUt: Double, timezone: Double): LocalDate {
        val amavasyaJD = lastAmavasyaBefore(jdSunriseUt - 1e-6)
        val swe = SweDate(amavasyaJD + timezone / 24.0)
        return LocalDate.of(swe.year, swe.month, swe.day)
    }

    private fun nextAmavasyaAfter(amavasyaUt: Double): Double {
        return refineConjunctionNewton(amavasyaUt + 29.5306)
    }

    fun calculateMonthInfo(
        jdSunriseToday: Double,
        timezone: Double,
        k: KundliData,
        tithiIndex: Int
    ): MonthInfo {

        // Step 1: last Amavasya (Moon-Sun conjunction) before today's sunrise
        val amavasyaJD = lastAmavasyaBefore(jdSunriseToday - 1e-6)

        // Month naming (Amanta) should be based on Sun's sidereal rashi at the Amavasya instant.
        // Using "first sunrise after Amavasya" can shift the month by 1 when Sankranti happens
        // between Amavasya and sunrise (common cause of mismatch vs JHora).
        val sunRashiAtAma = floor(solarLongitude(amavasyaJD) / 30.0).toInt().floorMod12()
        // JHora-style month naming (Amanta) uses a one-month offset relative to the Sun's rashi at Amavasya.
        // Without this, results often appear one month behind (e.g., Shravan instead of Bhadrvo).
        val maas = (RASHI_TO_GUJ_MAAS[sunRashiAtAma] + 1).floorMod12()

        // Step 2: next Amavasya
        val nextAmavasyaJD = nextAmavasyaAfter(amavasyaJD + 1e-6)

        // Adhik month: if Sun does NOT change sidereal rashi (no Sankranti) between two consecutive Amavasyas.
        // Use precise boundary-crossing time (not step scanning) for JHora-like stability.
        val sankrantiJd = nextSankrantiAfter(amavasyaJD)
        val isAdhik = sankrantiJd >= nextAmavasyaJD

        return MonthInfo(maas = maas, isAdhik = isAdhik)
    }
    // 3️⃣ HELPER: First sunrise after Amavasya
    private fun findFirstSunriseAfterAmavasya(amavasyaJD: Double, k: KundliData): Double {
        // Start from Amavasya day
        var testJD = floor(amavasyaJD - 0.5) + 0.5  // Noon of Amavasya day

        // Check up to 5 days ahead
        repeat(5) {
            // Extract date properly WITHOUT SweDate side effects
            val sweDate = SweDate(k.year, k.month, k.day, 0.0, SweDate.SE_GREG_CAL)
            var testJD = floor(amavasyaJD + 0.5)  // Start from Amavasya noon

            repeat(5) {
                val sunriseToday = sunRiseSetSwiss(
                    year = sweDate.year + (testJD - sweDate.julDay).toInt(),
                    month = sweDate.month,
                    day = sweDate.day + (testJD - sweDate.julDay).toInt(),
                    lon = k.longitude,
                    lat = k.latitude,
                    tz = k.timezone,
                    isRise = true
                )
                if (sunriseToday > amavasyaJD) return sunriseToday
                testJD += 1.0
            }
        }
        return testJD - 0.5  // Fallback
    }
    // 4️⃣ HELPER: Sankranti detection between Amavasyas
    private fun hasSankrantiBetween(startJD: Double, endJD: Double): Boolean {
        val step = 0.1  // 2.4 hours
        var prevRashi = raasi(startJD)

        var jd = startJD + step
        while (jd < endJD) {
            val currentRashi = raasi(jd)
            if (currentRashi != prevRashi) return true
            prevRashi = currentRashi
            jd += step
        }
        return false
    }

    private fun nextSankrantiAfter(jdStartUt: Double): Double {
        val sunLon0 = solarLongitude(jdStartUt)
        return findNextAngleMultipleUt(
            startJdUt = jdStartUt,
            startAngleDeg = sunLon0,
            segmentDeg = 30.0,
            angleProvider = ::solarLongitude,
            maxDaysAhead = 40.0
        )
    }

    private fun Int.floorMod12(): Int = ((this % 12) + 12) % 12
    /* ===================== SAMVATSARA ===================== */
    fun calculateVikramSamvat(
        k: KundliData,
        monthInfo: PanchangMonthInfo,
        pakshaInfo: PanchangPakshaInfo
    ): Int {
        var samvat = k.year + 57

        val isChaitra = monthInfo.maasIndex == 0
        val isShukla = pakshaInfo.paksha == "Shukla"

        // Vikram Samvat changes at Chaitra Shukla Pratipada
        if (isChaitra && isShukla) {
            samvat += 1
        }
        return samvat
    }

    /**
     * Index into [PanchangConstants.HORA_LORDS] for the **first hora** of this weekday
     * (Sun→0 … Saturn→4 per classic cycle Sun,Venus,Mercury,Moon,Saturn,Jupiter,Mars).
     */
    /** Panchang weekday date: before today's sunrise belongs to the previous civil date's vaar cycle. */
    fun panchangLocalDate(k: KundliData, birthJD: Double, sunriseJD: Double): LocalDate {
        val birthDate = LocalDate.of(k.year, k.month, k.day)
        return if (birthJD < sunriseJD) birthDate.minusDays(1) else birthDate
    }

    fun panchangWeekdayGuj(k: KundliData, birthJD: Double, sunriseJD: Double): String =
        gujaratiWeekday(panchangCalendarDayOfWeek(k, birthJD, sunriseJD))

    fun panchangCalendarDayOfWeek(k: KundliData, birthJD: Double, sunriseJD: Double): Int =
        when (panchangLocalDate(k, birthJD, sunriseJD).dayOfWeek) {
            DayOfWeek.SUNDAY -> Calendar.SUNDAY
            DayOfWeek.MONDAY -> Calendar.MONDAY
            DayOfWeek.TUESDAY -> Calendar.TUESDAY
            DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
            DayOfWeek.THURSDAY -> Calendar.THURSDAY
            DayOfWeek.FRIDAY -> Calendar.FRIDAY
            DayOfWeek.SATURDAY -> Calendar.SATURDAY
        }

    fun horaStartIndexForWeekday(calendarDayOfWeek: Int): Int {
        return when (calendarDayOfWeek) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 3
            Calendar.TUESDAY -> 6
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 1
            Calendar.SATURDAY -> 4
            else -> 0
        }
    }

    /** Weekday lord (vaar lord) — same as first hora lord of that day. */
    fun weekdayDayLordKey(calendarDayOfWeek: Int): String =
        PanchangConstants.HORA_LORDS[horaStartIndexForWeekday(calendarDayOfWeek)]

    /* ===================== BIRTHHORA ===================== */
    /**
     * @param firstHoraStartIndex index into [PanchangConstants.HORA_LORDS] for sunrise day’s first hora
     *        (use [horaStartIndexForWeekday] with birth date’s weekday).
     */
    fun findBirthHoraGujaratiDrik(
        birthJD: Double,
        sunriseJD: Double,
        sunsetJD: Double,
        nextSunriseJD: Double,
        firstHoraStartIndex: Int
    ): String {

        val horaLords = PanchangConstants.HORA_LORDS

        val isDayBirth = birthJD >= sunriseJD && birthJD < sunsetJD

        val horaLength: Double
        val horasElapsed: Int
        val startIndex: Int

        if (isDayBirth) {
            horaLength = (sunsetJD - sunriseJD) / 12.0
            horasElapsed = ((birthJD - sunriseJD) / horaLength).toInt()
            startIndex = firstHoraStartIndex
        } else {
            var nightStartJD: Double
            val nightWeekdayIndex: Int

            if (birthJD < sunriseJD) {
                nightStartJD = sunsetJD - 1.0
                nightWeekdayIndex = (firstHoraStartIndex + 6) % 7
            } else {
                nightStartJD = sunsetJD
                nightWeekdayIndex = firstHoraStartIndex
            }

            if (birthJD < nightStartJD) {
                nightStartJD -= 1.0
            }

            horaLength = (nextSunriseJD - sunsetJD) / 12.0
            horasElapsed = ((birthJD - nightStartJD) / horaLength).toInt()

            startIndex = nightWeekdayIndex
        }

        val horaIndex = ((startIndex + horasElapsed) % 7 + 7) % 7

        return horaLords[horaIndex]
    }
    /* ===================== PYTHON-STYLE HELPERS ===================== */
    fun sunRiseSetSwiss(
        year: Int,
        month: Int,
        day: Int,
        lon: Double,
        lat: Double,
        tz: Double,
        isRise: Boolean
    ): Double {

        // Use LOCAL midnight as start time, then convert to UT.
        // SwissEph returns the *next* rise/set after the start JD; starting at noon can push sunrise to next day.
        val jdLocal0 = SweDate(year, month, day, 0.0, SweDate.SE_GREG_CAL).julDay
        val jdUT = jdLocal0 - tz / 24.0

        val geopos = doubleArrayOf(lon, lat, 0.0)
        val tret = DblObj()
        val serr = StringBuffer()

        val rsmi = if (isRise)
            SweConst.SE_CALC_RISE
        else
            SweConst.SE_CALC_SET

        val flags = SweConst.SEFLG_SWIEPH

        val result = sw.swe_rise_trans_true_hor(
            jdUT,
            SweConst.SE_SUN,
            StringBuffer(),
            flags,
            rsmi,
            geopos,
            1013.25,
            15.0,
            0.0,
            tret,
            serr
        )

        if (result >= 0) {
            return tret.`val`
        }

        return jdUT
    }

    private fun moonRiseSetSwiss(
        year: Int,
        month: Int,
        day: Int,
        lon: Double,
        lat: Double,
        tz: Double,
        isRise: Boolean
    ): Double? {

        // Use LOCAL midnight converted to UT (important for matching desktop software rise/set output)
        // Local midnight JD:
        val jdLocal0 = SweDate(year, month, day, 0.0, SweDate.SE_GREG_CAL).julDay
        val jdUT = jdLocal0 - tz / 24.0

        val geopos = doubleArrayOf(lon, lat, 0.0)
        val tret = DblObj()
        val serr = StringBuffer()

        val rsmi = if (isRise) SweConst.SE_CALC_RISE else SweConst.SE_CALC_SET

        val flags = SweConst.SEFLG_SWIEPH or SweConst.SEFLG_SPEED

        val result = sw.swe_rise_trans_true_hor(
            jdUT,
            SweConst.SE_MOON,
            StringBuffer(),
            flags,
            rsmi,
            geopos,
            1013.25,
            15.0,
            0.0,
            tret,
            serr
        )

        return if (result >= 0) tret.`val` else null
    }

    /** Next moonrise JD (UT) after local midnight for the given calendar day, or null. */
    fun moonRiseJdUt(
        year: Int,
        month: Int,
        day: Int,
        lon: Double,
        lat: Double,
        tz: Double
    ): Double? = moonRiseSetSwiss(year, month, day, lon, lat, tz, true)

    private fun normalizeMoonToSunriseDay(jdEvent: Double, sunriseJdUT: Double): Double {
        var jd = jdEvent
        if (jd < sunriseJdUT - 0.5) jd += 1.0  // allow next-day morning
        return jd
    }

    private fun solarLongitude(jd: Double): Double {
        val flags =
            SweConst.SEFLG_SWIEPH or
                    SweConst.SEFLG_SIDEREAL

        val x = DoubleArray(6)
        val serr = StringBuffer()
        sw.swe_calc_ut(jd, SweConst.SE_SUN, flags, x, serr)

        return (x[0] % 360 + 360) % 360
    }

    private data class LonSpeed(val lon: Double, val speed: Double)

    private fun solarLonSpeed(jdUt: Double): LonSpeed {
        val flags = SweConst.SEFLG_SWIEPH or SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED
        val x = DoubleArray(6)
        val serr = StringBuffer()
        sw.swe_calc_ut(jdUt, SweConst.SE_SUN, flags, x, serr)
        val lon = (x[0] % 360 + 360) % 360
        val speed = x[3] // deg/day
        return LonSpeed(lon, speed)
    }

    private fun lunarLonSpeed(jdUt: Double): LonSpeed {
        val flags = SweConst.SEFLG_SWIEPH or SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED
        val x = DoubleArray(6)
        val serr = StringBuffer()
        sw.swe_calc_ut(jdUt, SweConst.SE_MOON, flags, x, serr)
        val lon = (x[0] % 360 + 360) % 360
        val speed = x[3] // deg/day
        return LonSpeed(lon, speed)
    }
    fun trueAmavasya(jd: Double): Double {

        var t = jd

        repeat(15) {
            val sun = solarLonSpeed(t)
            val moon = lunarLonSpeed(t)

            var diff = (moon.lon - sun.lon) % 360.0
            if (diff < 0) diff += 360.0
            if (diff > 180.0) diff -= 360.0  // now in [-180, +180]

            val relSpeed = (moon.speed - sun.speed).coerceAtLeast(1e-6) // deg/day
            val dt = diff / relSpeed
            t -= dt

            if (abs(diff) < 1e-6) return t
        }

        return t
    }
    private fun lunarLongitude(jd: Double): Double {
        val flags = SweConst.SEFLG_SWIEPH or
                SweConst.SEFLG_SIDEREAL or
                SweConst.SEFLG_SPEED

        val x = DoubleArray(6)
        val serr = StringBuffer()
        sw.swe_calc_ut(jd, SweConst.SE_MOON, flags, x, serr)

        return (x[0] % 360 + 360) % 360
    }

    private fun lunarPhase(jd: Double): Double =
        (lunarLongitude(jd) - solarLongitude(jd) + 360.0) % 360.0

    private fun yogaAngleDeg(jdUt: Double): Double {
        val lunar = lunarLongitude(jdUt)
        val solar = solarLongitude(jdUt)
        return (lunar + solar) % 360.0
    }

    /**
     * Finds the next time (UT JD) when angle crosses into the next segment boundary.
     * Assumes the angle increases monotonically over the short search window.
     */
    private fun findNextAngleMultipleUt(
        startJdUt: Double,
        startAngleDeg: Double,
        segmentDeg: Double,
        angleProvider: (Double) -> Double,
        maxDaysAhead: Double
    ): Double {
        val eps = 1e-9
        val startNorm = ((startAngleDeg % 360.0) + 360.0) % 360.0

        // Next boundary angle in [0, 360], but treat wrap by unwrapping > start
        var nextBoundary = ceil((startNorm + eps) / segmentDeg) * segmentDeg
        if (nextBoundary > 360.0 - 1e-10) nextBoundary = 360.0

        val targetUnwrapped = if (nextBoundary <= startNorm + 1e-12) nextBoundary + 360.0 else nextBoundary

        fun angleUnwrapped(jd: Double): Double {
            val a = ((angleProvider(jd) % 360.0) + 360.0) % 360.0
            return if (a + 1e-12 < startNorm) a + 360.0 else a
        }

        var t0 = startJdUt
        var t1 = startJdUt + 0.25 // start with 6 hours

        fun g(t: Double) = angleUnwrapped(t) - targetUnwrapped

        var g0 = g(t0)
        var g1 = g(t1)

        // Expand until we bracket the crossing or hit maxDaysAhead
        val maxT = startJdUt + maxDaysAhead
        while (g1 < 0.0 && t1 < maxT) {
            t1 += 0.25
            g1 = g(t1)
        }
        if (g1 < 0.0) return maxT

        // Binary search
        repeat(50) {
            val mid = (t0 + t1) / 2.0
            val gm = g(mid)
            if (gm >= 0.0) {
                t1 = mid
                g1 = gm
            } else {
                t0 = mid
                g0 = gm
            }
        }
        return t1
    }

    fun moonSunDiff(jd: Double): Double {
        val sun = solarLongitude(jd)
        val moon = lunarLongitude(jd)

        var diff = (moon - sun) % 360.0
        if (diff < 0) diff += 360.0

        return diff
    }

    fun newMoon(jdStart: Double): Double {
        var t1 = jdStart - 2.0
        var t2 = jdStart + 2.0
        var mid: Double

        fun diff(jd: Double): Double {
            val sun = solarLongitude(jd)
            val moon = lunarLongitude(jd)
            var d = (moon - sun) % 360.0
            if (d < 0) d += 360.0
            if (d > 180) d -= 360.0
            return d
        }

        repeat(40) {
            mid = (t1 + t2) / 2.0
            if (diff(t1) * diff(mid) <= 0) {
                t2 = mid
            } else {
                t1 = mid
            }
        }
        return (t1 + t2) / 2.0
    }

    fun raasi(jd: Double): Int {
        val lon = solarLongitude(jd)
        return (lon / 30.0).toInt() // 0 = Mesha ... 11 = Meena
    }

    private fun elapsedYear(jd: Double, masaNum: Int): Int {
        val ahargana = jd - 588465.5
        val siderealYear = 365.25636
        return ((ahargana + (4 - masaNum) * 30) / siderealYear).toInt()
    }

    private fun unwrapAngles(a: DoubleArray): DoubleArray {
        val r = a.clone()
        for (i in 1 until r.size) {
            if (r[i] < r[i - 1]) r[i] += 360.0
        }
        return r
    }

    private fun minutesToHHMM(totalMinutes: Int): String {
        val hh = totalMinutes / 60
        val mm = totalMinutes % 60
        return "%02d:%02d".format(hh, mm)
    }

    private fun inverseLagrange(x: DoubleArray, y: DoubleArray, ya: Double): Double {
        var total = 0.0
        for (i in x.indices) {
            var numer = 1.0
            var denom = 1.0
            for (j in x.indices) {
                if (i != j) {
                    numer *= (ya - y[j])
                    denom *= (y[i] - y[j])
                }
            }
            total += numer * x[i] / denom
        }
        return total
    }
}