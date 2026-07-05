package com.vidyarthi.lalkitab.utils

/**
 * Lal Kitab: life year (1 = first year after birth) maps to **khana** (1–12) in a repeating 36-year
 * pattern — 12 blocks of 3 years each, in fixed order (same as the user’s table).
 */
object RashifalKhanaAgeTable {

    /** One full 36-year cycle display order. */
    val KHANA_CYCLE_ORDER: IntArray = intArrayOf(1, 2, 3, 10, 11, 12, 4, 5, 6, 7, 8, 9)

    /** Display order for the reference table (matches common Lal Kitab sheets). */
    val KHANA_DISPLAY_ORDER: List<Int> = listOf(1, 2, 3, 10, 11, 12, 4, 5, 6, 7, 8, 9)

    /** Explicit mapping copied from user-provided table. */
    private val AGE_KHANA_RANGES: List<Pair<IntRange, Int>> = listOf(
        (1..3) to 1, (37..39) to 1, (73..75) to 1, (109..111) to 1,
        (4..6) to 2, (40..42) to 2, (76..78) to 2, (112..115) to 2,
        (7..9) to 3, (43..45) to 3, (79..81) to 3, (116..118) to 3,
        (10..12) to 10, (46..48) to 10, (82..84) to 10, (119..121) to 10,
        (13..15) to 11, (49..51) to 11, (85..87) to 11,
        (16..18) to 12, (52..54) to 12, (88..90) to 12,
        (19..21) to 4, (55..57) to 4, (91..93) to 4,
        (22..24) to 5, (58..60) to 5, (94..96) to 5,
        (25..27) to 6, (61..63) to 6, (97..99) to 6,
        (28..30) to 7, (64..66) to 7, (100..102) to 7,
        (31..33) to 8, (65..69) to 8, (103..105) to 8,
        (34..36) to 9, (70..72) to 9, (106..108) to 9
    )

    private val TABLE_AGE_COLUMNS: Map<Int, List<String>> = mapOf(
        1 to listOf("1-3", "37-39", "73-75", "109-111"),
        2 to listOf("4-6", "40-42", "76-78", "112-115"),
        3 to listOf("7-9", "43-45", "79-81", "116-118"),
        10 to listOf("10-12", "46-48", "82-84", "119-121"),
        11 to listOf("13-15", "49-51", "85-87", "—"),
        12 to listOf("16-18", "52-54", "88-90", "—"),
        4 to listOf("19-21", "55-57", "91-93", "—"),
        5 to listOf("22-24", "58-60", "94-96", "—"),
        6 to listOf("25-27", "61-63", "97-99", "—"),
        7 to listOf("28-30", "64-66", "100-102", "—"),
        8 to listOf("31-33", "65-69", "103-105", "—"),
        9 to listOf("34-36", "70-72", "106-108", "—")
    )

    fun khanaForLifeYear(lifeYear: Int): Int {
        val y = lifeYear.coerceAtLeast(1)
        for ((range, khana) in AGE_KHANA_RANGES) {
            if (y in range) return khana
        }
        return KHANA_CYCLE_ORDER[((y - 1) / 3) % 12]
    }

    /** The 3-year age band that contains [lifeYear] (e.g. 35 → "34–36"). */
    fun lifeYearBandLabel(lifeYear: Int): String {
        val y = lifeYear.coerceAtLeast(1)
        val slot = ((y - 1) / 3)
        val a = slot * 3 + 1
        val b = a + 2
        return "$a–$b"
    }

    data class TableRow(
        val khana: Int,
        val colAges: List<String>
    )

    /**
     * Four columns: years 1–36, 37–72, 73–108, 109–120 (last column trimmed if needed).
     */
    fun referenceTableRows(maxLifeYear: Int = 120): List<TableRow> {
        return KHANA_DISPLAY_ORDER.map { khana ->
            val cols = TABLE_AGE_COLUMNS[khana] ?: listOf("—", "—", "—", "—")
            TableRow(khana, cols)
        }
    }
}

