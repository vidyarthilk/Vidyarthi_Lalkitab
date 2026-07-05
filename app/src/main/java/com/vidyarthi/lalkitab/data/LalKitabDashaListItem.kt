package com.vidyarthi.lalkitab.data

sealed class LalKitabDashaListItem {

    data class Mahadasha(val segment: LalKitabDashaSegment) : LalKitabDashaListItem()

    data class Antardasha(
        val mahadashaPlanet: String,
        val antarPlanet: String,
        val startFrac: Double,
        val endFrac: Double
    ) : LalKitabDashaListItem()
}
