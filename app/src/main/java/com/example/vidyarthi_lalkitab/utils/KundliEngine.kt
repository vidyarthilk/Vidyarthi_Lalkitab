package com.example.vidyarthi_lalkitab.utils

import com.example.vidyarthi_lalkitab.data.*
import swisseph.*
import kotlin.math.floor

object KundliEngine {

    private val RASHI = arrayOf(
        "મેષ","વૃષભ","મિથુન","કર્ક","સિંહ","કન્યા",
        "તુલા","વૃશ્ચિક","ધનુ","મકર","કુંભ","મીન"
    )

    fun calculate(k: KundliData): KundliChart {

        // ── Time → UT ──
        val utTime = TimeUtils.localToUT(
            k.hour, k.minute, k.timezone
        )

        val jd = SweDate(
            k.year, k.month, k.day,
            utTime,
            SweDate.SE_GREG_CAL
        ).julDay

        // ── Houses & Lagna ──
        val houses = DoubleArray(13)
        val ascmc = DoubleArray(10)

        SwissEphManager.sw.swe_houses(
            jd,
            SweConst.SEFLG_SIDEREAL,   // ✅ REQUIRED FLAGS
            k.latitude,
            k.longitude,
            'P'.code,                 // Placidus
            houses,
            ascmc
        )


        val lagnaLon = ascmc[0]
        val lagnaRashi = RASHI[(lagnaLon / 30).toInt() % 12]

        // ── Graha Positions ──
        val grahas = mutableListOf<GrahaPosition>()

        addGraha(grahas, "સૂર્ય", SweConst.SE_SUN, jd, houses)
        addGraha(grahas, "ચંદ્ર", SweConst.SE_MOON, jd, houses)
        addGraha(grahas, "મંગળ", SweConst.SE_MARS, jd, houses)
        addGraha(grahas, "બુધ", SweConst.SE_MERCURY, jd, houses)
        addGraha(grahas, "ગુરુ", SweConst.SE_JUPITER, jd, houses)
        addGraha(grahas, "શુક્ર", SweConst.SE_VENUS, jd, houses)
        addGraha(grahas, "શનિ", SweConst.SE_SATURN, jd, houses)
        addGraha(grahas, "રાહુ", SweConst.SE_MEAN_NODE, jd, houses)

        return KundliChart(
            lagnaRashi = lagnaRashi,
            lagnaDegree = lagnaLon % 30,
            houses = houses.drop(1),
            grahas = grahas
        )
    }

    private fun addGraha(
        list: MutableList<GrahaPosition>,
        name: String,
        planet: Int,
        jd: Double,
        houses: DoubleArray
    ) {
        val pos = DoubleArray(6)

        SwissEphManager.sw.swe_calc(
            jd,
            planet,
            SweConst.SEFLG_SIDEREAL,
            pos,
            null
        )

        val lon = pos[0]
        val rashiIndex = floor(lon / 30).toInt() % 12
        val house = getHouse(lon, houses)

        list.add(
            GrahaPosition(
                name = name,
                rashi = RASHI[rashiIndex],
                degree = lon % 30,
                house = house
            )
        )
    }

    private fun getHouse(lon: Double, houses: DoubleArray): Int {
        for (i in 1..12) {
            val start = houses[i]
            val end = houses[if (i == 12) 1 else i + 1]
            if (lon >= start && lon < end) return i
        }
        return 12
    }
}
