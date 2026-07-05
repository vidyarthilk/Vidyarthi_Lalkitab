package com.vidyarthi.lalkitab.data

/**
 * @param lagnaHouse 1-based લગ્ન રાશિ ક્રમ (મેષ=1 … મીન=12) — ચાર્ટ પર ખાનું 1 માં જે રાશિ દેખાડવી.
 */
data class KundliChart(
    val lagnaRashi: String,
    val lagnaDegree: Double,
    val lagnaHouse: Int,
    val houses: List<Double>,
    val grahas: List<GrahaPosition>
)
