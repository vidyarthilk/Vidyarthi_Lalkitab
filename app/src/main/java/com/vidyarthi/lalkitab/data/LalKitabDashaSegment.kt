package com.vidyarthi.lalkitab.data

data class LalKitabDashaSegment(
    val startYear: Int,
    val endYear: Int,
    val planetName: String,
    /** Full period in the 35-year table (e.g. 6 for Mangal). */
    val standardYears: Int,
    val isPartial: Boolean
)

data class LalKitabDashaResult(
    /** Graha (Gujarati name) used as janma samay graha for dasha. */
    val samayGrahaName: String,
    /** That graha’s **khana** (1–12) in the chart used for this result (janma or chandra). */
    val samayGrahaHouse: Int,
    /** First life-year when varshfal table maps [samayGrahaHouse] → khana 1; mahadasha starts this year. */
    val mainDashaStartYear: Int,
    val segments: List<LalKitabDashaSegment>
)
