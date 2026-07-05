package com.vidyarthi.lalkitab.utils

import android.content.Context

object VarshfalTable {

    private val table: MutableMap<Int, IntArray> = mutableMapOf()

    fun load(context: Context) {
        if (table.isNotEmpty()) return

        val inputStream = context.assets.open("varshfal.csv")
        val lines = inputStream.bufferedReader().readLines()

        for (line in lines.drop(1)) { // skip header
            val parts = line.split(",")

            if (parts.size < 13) continue

            val age = parts[0].toInt()

            val houses = IntArray(12) {
                parts[it + 1].toInt()
            }

            table[age] = houses
        }
    }

    fun getRow(age: Int): IntArray? {
        return table[age]
    }

    /**
     * Smallest life-year **age** (1…) such that [VarshfalTable] maps planets in [birthHouse] to **khana 1**
     * (same rule as [KundliEngine.applyLalKitabVarshfal] for that age). Used for Lal Kitab dasha start year.
     */
    fun firstLifeYearWhenHouseMapsToKhanaOne(birthHouse: Int, maxAge: Int = 120): Int? {
        val h = birthHouse.coerceIn(1, 12)
        val col = h - 1
        for (age in 1..maxAge) {
            val row = table[age] ?: continue
            if (col in row.indices && row[col] == 1) return age
        }
        return null
    }
}