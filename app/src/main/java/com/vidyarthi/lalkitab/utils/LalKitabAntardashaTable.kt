package com.vidyarthi.lalkitab.utils

/**
 * Lal Kitab: within each mahadasha, three antardashas (equal time each).
 * Names match [LalKitabDashaCalculator] cycle (Gujarati).
 *
 */
object LalKitabAntardashaTable {

    private val TRIPLES: Map<String, List<String>> = mapOf(
        "ગુરુ" to listOf("કેતુ", "ગુરુ", "સૂર્ય"),
        "સૂર્ય" to listOf("સૂર્ય", "ચંદ્ર", "મંગળ"),
        "ચંદ્ર" to listOf("ગુરુ", "સૂર્ય", "ચંદ્ર"),
        "શુક્ર" to listOf("મંગળ", "શુક્ર", "બુધ"),
        "મંગળ" to listOf("મંગળ", "શનિ", "શુક્ર"),
        "બુધ" to listOf("ચંદ્ર", "મંગળ", "ગુરુ"),
        "શનિ" to listOf("રાહુ", "બુધ", "શનિ"),
        "રાહુ" to listOf("મંગળ", "કેતુ", "રાહુ"),
        "કેતુ" to listOf("શનિ", "રાહુ", "કેતુ")
    )

    fun tripleFor(mahadashaPlanet: String): List<String>? = TRIPLES[mahadashaPlanet]
}
