package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.data.LalKitabDashaResult
import com.vidyarthi.lalkitab.data.LalKitabDashaSegment

/**
 * Lal Kitab 35-year dasha cycle (fixed order & years).
 * [samayGrahaName]: janma samay graha (Gujarati name). Its **khana** in the chosen chart is used with
 * [VarshfalTable.firstLifeYearWhenHouseMapsToKhanaOne]: the **first life-year** when varshfal maps that khana to **1**
 * is when this graha’s mahadasha starts (janma vs chandra uses that chart’s house, same graha name).
 */
object LalKitabDashaCalculator {

    const val MOON_NAME = "ચંદ્ર"
    const val DEFAULT_MAX_YEAR = 120

    private data class P(val name: String, val years: Int)

    private val CYCLE = listOf(
        P("ગુરુ", 6),
        P("સૂર્ય", 2),
        P("ચંદ્ર", 1),
        P("શુક્ર", 3),
        P("મંગળ", 6),
        P("બુધ", 2),
        P("શનિ", 6),
        P("રાહુ", 6),
        P("કેતુ", 3)
    )

    /**
     * @param useChandraChartForHouse If true, samay graha’s house is read from the Moon (Lal Kitab) chart; if false, from the birth chart.
     */
    fun compute(
        k: KundliData,
        samayGrahaName: String = KundliHolder.dashaSamayGrahaName,
        maxYear: Int = DEFAULT_MAX_YEAR,
        useChandraChartForHouse: Boolean = false
    ): LalKitabDashaResult? {
        val chart = if (useChandraChartForHouse) {
            KundliEngine.calculateChandraKundli(k)
        } else {
            KundliEngine.calculate(k)
        }
        val graha = chart.grahas.firstOrNull { it.name == samayGrahaName } ?: return null
        val h = graha.house.coerceIn(1, 12)
        val dashStartYear = VarshfalTable.firstLifeYearWhenHouseMapsToKhanaOne(h, maxYear) ?: h
        val idx = CYCLE.indexOfFirst { it.name == samayGrahaName }
        if (idx < 0) return null

        val segments = mutableListOf<LalKitabDashaSegment>()
        var y = 1

        var remaining = dashStartYear - 1
        if (remaining > 0) {
            val chunkMeta = mutableListOf<Triple<String, Int, Int>>()
            var ci = (idx - 1 + CYCLE.size) % CYCLE.size
            while (remaining > 0) {
                val p = CYCLE[ci]
                val take = minOf(p.years, remaining)
                chunkMeta.add(Triple(p.name, take, p.years))
                remaining -= take
                ci = (ci - 1 + CYCLE.size) % CYCLE.size
            }
            chunkMeta.reverse()
            for ((name, take, full) in chunkMeta) {
                val sy = y
                val ey = y + take - 1
                segments.add(
                    LalKitabDashaSegment(
                        startYear = sy,
                        endYear = ey,
                        planetName = name,
                        standardYears = full,
                        isPartial = take < full
                    )
                )
                y = ey + 1
            }
        }

        if (y != dashStartYear) return null

        val ownP = CYCLE[idx]
        run {
            val sy = y
            val ey = minOf(y + ownP.years - 1, maxYear)
            val runLen = ey - sy + 1
            if (runLen > 0) {
                segments.add(
                    LalKitabDashaSegment(
                        startYear = sy,
                        endYear = ey,
                        planetName = ownP.name,
                        standardYears = ownP.years,
                        isPartial = runLen < ownP.years
                    )
                )
                y = ey + 1
            }
        }

        var ci = (idx + 1) % CYCLE.size
        while (y <= maxYear) {
            val p = CYCLE[ci]
            val take = minOf(p.years, maxYear - y + 1)
            val sy = y
            val ey = y + take - 1
            segments.add(
                LalKitabDashaSegment(
                    startYear = sy,
                    endYear = ey,
                    planetName = p.name,
                    standardYears = p.years,
                    isPartial = take < p.years
                )
            )
            y = ey + 1
            ci = (ci + 1) % CYCLE.size
        }

        return LalKitabDashaResult(
            samayGrahaName = samayGrahaName,
            samayGrahaHouse = h,
            mainDashaStartYear = dashStartYear,
            segments = segments
        )
    }
}
