package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.GrahaPosition
import com.vidyarthi.lalkitab.data.KundliChart

/**
 * લાલ કિતાબ **બુધનો સ્વભાવ**: દરેક ગ્રહના ખાના સાથે નિયત અંક ગુણાકાર, સરવાળો, 9 વડે ભાગ કરી શેષ.
 * શેષ 1…8 → તે અંકવાળો ગ્રહ; શેષ 0 → ખાનું 5ના ગ્રહો (ખાલી હોય તો સૂર્ય).
 */
object BudhSvabhavCalculator {

    private val WEIGHT_BY_NAME: Map<String, Int> = mapOf(
        "સૂર્ય" to 9,
        "ચંદ્ર" to 8,
        "શુક્ર" to 7,
        "ગુરુ" to 6,
        "મંગળ" to 5,
        "બુધ" to 4,
        "શનિ" to 3,
        "રાહુ" to 2,
        "કેતુ" to 1
    )

    /** શેષ 1 = કેતુ … 8 = ચંદ્ર */
    private val REMAINDER_TO_PLANET: Map<Int, String> = mapOf(
        1 to "કેતુ",
        2 to "રાહુ",
        3 to "શનિ",
        4 to "બુધ",
        5 to "મંગળ",
        6 to "ગુરુ",
        7 to "શુક્ર",
        8 to "ચંદ્ર"
    )

    data class Result(
        /** સ્વભાવ માટેના ગ્રહ(ો) — આંતરિક ગુજરાતી નામ */
        val svabhavGrahaKeys: List<String>,
        val remainder: Int,
        val weightedSum: Int
    )

    fun compute(chart: KundliChart): Result = compute(chart.grahas)

    fun compute(grahas: List<GrahaPosition>): Result {
        var sum = 0
        for (g in grahas) {
            val w = WEIGHT_BY_NAME[g.name] ?: continue
            val h = g.house.coerceIn(1, 12)
            sum += w * h
        }
        val shesh = sum % 9
        val keys = when (shesh) {
            0 -> {
                val h5 = grahas.filter { it.house == 5 }.map { it.name }.distinct()
                if (h5.isEmpty()) listOf("સૂર્ય") else h5
            }
            else -> listOf(REMAINDER_TO_PLANET.getValue(shesh))
        }
        return Result(keys, shesh, sum)
    }
}
