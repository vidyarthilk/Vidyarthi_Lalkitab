package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.KundliChart
import com.vidyarthi.lalkitab.data.KundliData

/**
 * Lal Kitab «Dhoka na grah»: houses 8 / 9 / 12 with defaults if empty.
 *
 * Year mapping: year-1 => house 1, year-2 => house 2 ... year-12 => house 12, then repeats.
 * Dhoka sequence is rotated from Surya's actual house.
 */
object DhokaNaGrahHelper {

    private const val SURYA = "સૂર્ય"
    private const val CHANDRA = "ચંદ્ર"
    private const val KETU = "કેતુ"
    private const val MANGAL = "મંગળ"
    private const val BUDH = "બુધ"
    private const val SHANI = "શનિ"
    private const val RAHU = "રાહુ"
    private const val GURU = "ગુરુ"
    private const val SHUKRA = "શુક્ર"

    private val DEFAULT_H8 = listOf("મંગળ", "શનિ")
    private val DEFAULT_H9 = listOf("ગુરુ")
    private val DEFAULT_H12 = listOf("ગુરુ")

    /** After Surya anchor: Chandra → Ketu → Mangal → Budh → Shani → Rahu (reference order). */
    val SEQUENCE_AFTER_SURYA: List<String> = listOf(
        "ચંદ્ર", "કેતુ", "મંગળ", "બુધ", "શનિ", "રાહુ"
    )

    data class DhokaChartResult(
        /** Surya’s actual house in this chart (1–12). */
        val sunHouseActual: Int,
        /** House used as «first year» start / year-reference anchor after 9↔6 and 7↔5 rules. */
        val firstYearAnchorHouse: Int,
        val grahasHouse8: List<String>,
        val usedDefault8: Boolean,
        val grahasHouse9: List<String>,
        val usedDefault9: Boolean,
        val grahasHouse12: List<String>,
        val usedDefault12: Boolean
    )

    data class DhokaYearResult(
        val lifeYear: Int,
        val firstYearAnchorHouse: Int,
        val grahaKeys: List<String>
    )

    private fun grahasInHouse(chart: KundliChart, house: Int): List<String> {
        return chart.grahas
            .filter { it.house == house }
            .map { it.name }
            .distinct()
    }

    /** First-year reference uses Surya's actual house (1..12). */
    fun resolveFirstYearAnchor(sunHouse: Int, house1Empty: Boolean): Int = sunHouse.coerceIn(1, 12)

    fun compute(chart: KundliChart): DhokaChartResult {
        val sunHouseActual = chart.grahas.firstOrNull { it.name == SURYA }?.house?.coerceIn(1, 12) ?: 1
        val house1Empty = grahasInHouse(chart, 1).isEmpty()
        val anchor = resolveFirstYearAnchor(sunHouseActual, house1Empty)

        val used8 = true
        val out8 = DEFAULT_H8

        val g9 = grahasInHouse(chart, 9)
        val used9 = g9.isEmpty()
        val out9 = if (used9) DEFAULT_H9 else g9

        val g12 = grahasInHouse(chart, 12)
        val used12 = g12.isEmpty()
        val out12 = if (used12) DEFAULT_H12 else g12

        return DhokaChartResult(
            sunHouseActual = sunHouseActual,
            firstYearAnchorHouse = anchor,
            grahasHouse8 = out8,
            usedDefault8 = used8,
            grahasHouse9 = out9,
            usedDefault9 = used9,
            grahasHouse12 = out12,
            usedDefault12 = used12
        )
    }

    /**
     * Dhoka graha logic:
     * - Year 1 maps to house 1, year N maps to ((N-1)%12)+1.
     * - Sequence Surya..house12 is rotated by Surya's actual house.
     */
    fun computeForLifeYear(chart: KundliChart, lifeYear: Int): DhokaYearResult {
        val base = compute(chart)
        val y = lifeYear.coerceIn(1, 120)
        val sequenceBySunStart: List<List<String>> = listOf(
            listOf(SURYA),
            listOf(CHANDRA),
            listOf(KETU),
            listOf(MANGAL),
            listOf(BUDH),
            listOf(SHANI),
            listOf(RAHU),
            base.grahasHouse8,
            base.grahasHouse9,
            listOf(GURU),
            listOf(SHUKRA),
            base.grahasHouse12
        )
        val yearHouse = ((y - 1) % 12) + 1
        val idx = Math.floorMod(yearHouse - base.sunHouseActual, sequenceBySunStart.size)
        return DhokaYearResult(
            lifeYear = y,
            firstYearAnchorHouse = base.sunHouseActual,
            grahaKeys = sequenceBySunStart[idx]
        )
    }

    fun forJanma(k: KundliData): DhokaChartResult = compute(KundliEngine.calculate(k))

    fun forChandra(k: KundliData): DhokaChartResult = compute(KundliEngine.calculateChandraKundli(k))
}
