package com.vidyarthi.lalkitab.utils

import android.content.Context
import com.vidyarthi.lalkitab.data.GrahaPosition
import com.vidyarthi.lalkitab.data.KundliChart
import com.vidyarthi.lalkitab.data.KundliData
import swisseph.SweConst
import kotlin.math.floor

object KundliEngine {

    private val RASHI = arrayOf(
        "મેષ", "વૃષભ", "મિથુન", "કર્ક", "સિંહ", "કન્યા",
        "તુલા", "વૃશ્ચિક", "ધનુ", "મકર", "કુંભ", "મીન"
    )

    fun calculate(k: KundliData): KundliChart {
        val jd = TimeUtils.julianDayUtcForBirth(k)

        val houses = DoubleArray(13)
        val ascmc = DoubleArray(10)

        SwissEphManager.sw.swe_houses(
            jd,
            SweConst.SEFLG_SIDEREAL,
            k.latitude,
            k.longitude,
            'P'.code,
            houses,
            ascmc
        )

        val lagnaLon = normalize360(ascmc[0])
        val lagnaRashiIndex = floor(lagnaLon / 30.0).toInt().floorMod12()
        val lagnaRashi = RASHI[lagnaRashiIndex]

        val grahas = mutableListOf<GrahaPosition>()
        addGraha(grahas, "સૂર્ય", SweConst.SE_SUN, jd, lagnaRashiIndex)
        addGraha(grahas, "ચંદ્ર", SweConst.SE_MOON, jd, lagnaRashiIndex)
        addGraha(grahas, "મંગળ", SweConst.SE_MARS, jd, lagnaRashiIndex)
        addGraha(grahas, "બુધ", SweConst.SE_MERCURY, jd, lagnaRashiIndex)
        addGraha(grahas, "ગુરુ", SweConst.SE_JUPITER, jd, lagnaRashiIndex)
        addGraha(grahas, "શુક્ર", SweConst.SE_VENUS, jd, lagnaRashiIndex)
        addGraha(grahas, "શનિ", SweConst.SE_SATURN, jd, lagnaRashiIndex)

        val rahuLon = planetLongitude(SweConst.SE_MEAN_NODE, jd)
        addGrahaFromLongitude(grahas, "રાહુ", rahuLon, lagnaRashiIndex)
        addGrahaFromLongitude(grahas, "કેતુ", normalize360(rahuLon + 180.0), lagnaRashiIndex)

        return KundliChart(
            lagnaRashi = lagnaRashi,
            lagnaDegree = lagnaLon % 30,
            lagnaHouse = lagnaRashiIndex + 1,
            houses = houses.drop(1),
            grahas = grahas
        )
    }

    /**
     * લાલ કિતાબ ચંદ્ર કુંડળી: ચંદ્રને લગ્ન રાશિ નંબર જેટલા ખાને; ફ્રેમ માટે [lagnaHouse]=1 (મેષ ફિક્સ ચાર્ટ લેબલ [KundliChartView] માં).
     */
    fun calculateChandraKundli(k: KundliData): KundliChart {
        val janma = calculate(k)
        val moonJanma = janma.grahas.firstOrNull { it.name == "ચંદ્ર" } ?: return janma
        val moonJanmaHouse = moonJanma.house
        val lagnaRashiNumber = janma.lagnaHouse.coerceIn(1, 12)
        val moonTargetKhana = lagnaRashiNumber

        val newGrahas = janma.grahas.map { graha ->
            val offset = (graha.house - moonJanmaHouse + 12) % 12
            val newHouse = ((moonTargetKhana - 1 + offset) % 12) + 1
            graha.copy(house = newHouse)
        }

        return KundliChart(
            lagnaRashi = janma.lagnaRashi,
            lagnaDegree = janma.lagnaDegree,
            lagnaHouse = 1,
            houses = janma.houses,
            grahas = newGrahas
        )
    }

    fun applyLalKitabVarshfal(
        context: Context,
        grahas: List<GrahaPosition>,
        age: Int
    ): List<GrahaPosition> {
        VarshfalTable.load(context)
        val row = VarshfalTable.getRow(age) ?: return grahas
        return grahas.map { graha ->
            val oldHouse = graha.house
            val newHouse = row[oldHouse - 1]
            graha.copy(house = newHouse)
        }
    }

    fun getVarshfalYear(dob: java.time.LocalDateTime): Int {
        val now = java.time.LocalDateTime.now()
        val birthdayThisYear = dob.withYear(now.year)
        var years = now.year - dob.year
        if (now.isBefore(birthdayThisYear)) {
            years--
        }
        return years + 1
    }

    fun getCurrentVarshfalYear(dob: java.time.LocalDateTime): Int = getVarshfalYear(dob)

    private fun getLastBirthday(dob: java.time.LocalDateTime): java.time.LocalDateTime {
        val now = java.time.LocalDateTime.now()
        var lastBirthday = dob.withYear(now.year)
        if (now.isBefore(lastBirthday)) {
            lastBirthday = lastBirthday.minusYears(1)
        }
        return lastBirthday
    }

    fun startOfCurrentDivasDay(
        dob: java.time.LocalDateTime,
        now: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ): java.time.LocalDateTime {
        val candidate = now.toLocalDate().atTime(dob.hour, dob.minute, dob.second, dob.nano)
        return if (now.isBefore(candidate)) candidate.minusDays(1) else candidate
    }

    fun getBirthAlignedHourIndex(
        dob: java.time.LocalDateTime,
        now: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ): Int {
        val start = startOfCurrentDivasDay(dob, now)
        val minutes = java.time.temporal.ChronoUnit.MINUTES.between(start, now).coerceAtLeast(0)
        val minutesInDay = (minutes % (24 * 60)).toInt()
        return (minutesInDay / 60).coerceIn(0, 23)
    }

    fun formatKalakSlotRangeLabel(
        dob: java.time.LocalDateTime,
        slotIndex: Int,
        now: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ): String {
        val dayStart = startOfCurrentDivasDay(dob, now)
        val hStart = dayStart.plusHours(slotIndex.toLong())
        val hEnd = hStart.plusHours(1)
        val df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val tf = java.time.format.DateTimeFormatter.ofPattern("H:mm")
        return if (hStart.toLocalDate() == hEnd.toLocalDate()) {
            "${hStart.format(df)} · ${hStart.format(tf)}–${hEnd.format(tf)}"
        } else {
            "${hStart.format(df)} ${hStart.format(tf)} – ${hEnd.format(df)} ${hEnd.format(tf)}"
        }
    }

    fun applyMonthlySunShift(
        varshfalGrahas: List<GrahaPosition>,
        lagna: Int,
        month: Int
    ): Pair<List<GrahaPosition>, Int> {
        val sunVarshfal = varshfalGrahas.find { it.name == "સૂર્ય" } ?: return Pair(varshfalGrahas, lagna)
        val originalSunHouse = sunVarshfal.house
        val newSunHouse = month
        val shift = (newSunHouse - originalSunHouse + 12) % 12
        val newGrahas = varshfalGrahas.map { g ->
            var nHouse = (g.house + shift) % 12
            if (nHouse == 0) nHouse = 12
            g.copy(house = nHouse)
        }
        return Pair(newGrahas, lagna)
    }

    fun applyDivasKundliShift(
        maasGrahas: List<GrahaPosition>,
        lagna: Int,
        day: Int
    ): Pair<List<GrahaPosition>, Int> {
        val mangal = maasGrahas.find { it.name == "મંગળ" } ?: return Pair(maasGrahas, lagna)
        val shift = (day - 1) % 12
        val newGrahas = maasGrahas.map { g ->
            var nHouse = g.house + shift
            if (nHouse > 12) nHouse -= 12
            g.copy(house = nHouse)
        }
        return Pair(newGrahas, lagna)
    }

    fun applyKalakKundliShift(
        divasGrahas: List<GrahaPosition>,
        lagna: Int,
        hourIndex: Int
    ): Pair<List<GrahaPosition>, Int> {
        if (divasGrahas.none { it.name == "ગુરુ" }) return Pair(divasGrahas, lagna)
        val shift = hourIndex % 12
        val newGrahas = divasGrahas.map { g ->
            var nHouse = g.house + shift
            if (nHouse > 12) nHouse -= 12
            g.copy(house = nHouse)
        }
        return Pair(newGrahas, lagna)
    }

    fun getVarshfalMonth(dob: java.time.LocalDateTime): Int {
        val now = java.time.LocalDateTime.now()
        val lastBirthday = getLastBirthday(dob)
        var months = (now.year - lastBirthday.year) * 12 + (now.monthValue - lastBirthday.monthValue)
        val currentMonthStart = lastBirthday.plusMonths(months.toLong())
        if (now.isBefore(currentMonthStart)) {
            months--
        }
        return months + 1
    }

    fun getVarshfalDayIndexInCurrentMonth(
        dob: java.time.LocalDateTime,
        now: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ): Int {
        var age = now.year - dob.year
        if (now.isBefore(dob.withYear(now.year))) {
            age--
        }
        val varshStart = dob.plusYears(age.toLong())
        val currentMonth = getVarshfalMonth(dob)
        val monthStart = varshStart.plusMonths((currentMonth - 1).toLong())
        val monthEnd = monthStart.plusMonths(1)
        val daysInMonth = java.time.temporal.ChronoUnit.DAYS.between(monthStart, monthEnd).toInt().coerceAtLeast(1)
        val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(monthStart, now).toInt()
        return (elapsedDays + 1).coerceIn(1, daysInMonth)
    }

    fun getFormattedRange(start: java.time.LocalDateTime, end: java.time.LocalDateTime): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return "${start.format(formatter)} - ${end.format(formatter)}"
    }

    fun getVarshfalMonthRange(
        dob: java.time.LocalDateTime,
        month: Int
    ): Pair<java.time.LocalDateTime, java.time.LocalDateTime> {
        val now = java.time.LocalDateTime.now()
        var age = now.year - dob.year
        if (now.isBefore(dob.withYear(now.year))) {
            age--
        }
        val varshStart = dob.plusYears(age.toLong())
        val start = varshStart.plusMonths((month - 1).toLong())
        val end = start.plusMonths(1)
        return Pair(start, end)
    }

    fun getDivasDateRangeFromMonth(
        dob: java.time.LocalDateTime,
        month: Int,
        day: Int
    ): Pair<java.time.LocalDateTime, java.time.LocalDateTime> {
        val now = java.time.LocalDateTime.now()
        var age = now.year - dob.year
        if (now.isBefore(dob.withYear(now.year))) {
            age--
        }
        val varshStart = dob.plusYears(age.toLong())
        val monthStart = varshStart.plusMonths((month - 1).toLong())
        val start = monthStart.plusDays((day - 1).toLong())
        val end = start.plusDays(1)
        return Pair(start, end)
    }

    fun getVarshfalDateRange(
        dob: java.time.LocalDateTime,
        year: Int
    ): Pair<java.time.LocalDateTime, java.time.LocalDateTime> {
        val start = dob.plusYears((year - 1).toLong())
        val end = dob.plusYears(year.toLong())
        return Pair(start, end)
    }

    private fun planetLongitude(planet: Int, jd: Double): Double {
        val pos = DoubleArray(6)
        SwissEphManager.sw.swe_calc(
            jd,
            planet,
            SweConst.SEFLG_SIDEREAL,
            pos,
            null
        )
        return normalize360(pos[0])
    }

    private fun addGraha(
        list: MutableList<GrahaPosition>,
        name: String,
        planet: Int,
        jd: Double,
        lagnaRashiIndex: Int
    ) {
        val lon = planetLongitude(planet, jd)
        val rashiIndex = floor(lon / 30.0).toInt().floorMod12()
        val house = wholeSignHouse(rashiIndex, lagnaRashiIndex)
        list.add(
            GrahaPosition(
                name = name,
                rashi = RASHI[rashiIndex],
                degree = lon % 30,
                house = house
            )
        )
    }

    private fun addGrahaFromLongitude(
        list: MutableList<GrahaPosition>,
        name: String,
        lon: Double,
        lagnaRashiIndex: Int
    ) {
        val lonN = normalize360(lon)
        val rashiIndex = floor(lonN / 30.0).toInt().floorMod12()
        val house = wholeSignHouse(rashiIndex, lagnaRashiIndex)
        list.add(
            GrahaPosition(
                name = name,
                rashi = RASHI[rashiIndex],
                degree = lonN % 30,
                house = house
            )
        )
    }

    private fun wholeSignHouse(rashiIndex: Int, lagnaRashiIndex: Int): Int =
        ((rashiIndex - lagnaRashiIndex + 12) % 12) + 1

    private fun normalize360(x: Double): Double = ((x % 360.0) + 360.0) % 360.0

    private fun Int.floorMod12(): Int = ((this % 12) + 12) % 12
}
