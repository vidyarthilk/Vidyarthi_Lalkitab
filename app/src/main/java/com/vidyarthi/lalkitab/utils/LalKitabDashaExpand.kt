package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.LalKitabDashaListItem
import com.vidyarthi.lalkitab.data.LalKitabDashaSegment
object LalKitabDashaExpand {

    /** Three equal antardasha slices for [seg] (empty if no triple or zero length). */
    fun antardashasForSegment(seg: LalKitabDashaSegment): List<LalKitabDashaListItem.Antardasha> {
        val triple = LalKitabAntardashaTable.tripleFor(seg.planetName)
        if (triple == null || triple.size != 3) return emptyList()
        // Life-year boundaries: year N starts at N, and ends at boundary N+1.
        // So segment [startYear..endYear] spans [startYear, endYear + 1) in fractional years.
        val start = seg.startYear.toDouble()
        val endBoundary = seg.endYear.toDouble() + 1.0
        val len = endBoundary - start
        if (len <= 0) return emptyList()
        val step = len / 3.0
        return (0..2).map { i ->
            val lo = start + i * step
            val hiDisplay = if (i == 2) endBoundary else start + (i + 1) * step - 0.01
            LalKitabDashaListItem.Antardasha(
                mahadashaPlanet = seg.planetName,
                antarPlanet = triple[i],
                startFrac = lo,
                endFrac = hiDisplay
            )
        }
    }

    /**
     * Flattens mahadashas with antardashas (legacy flat list; prefer expandable UI).
     */
    fun toListItems(segments: List<LalKitabDashaSegment>): List<LalKitabDashaListItem> {
        val out = mutableListOf<LalKitabDashaListItem>()
        for (seg in segments) {
            out.add(LalKitabDashaListItem.Mahadasha(seg))
            antardashasForSegment(seg).forEach { out.add(it) }
        }
        return out
    }
}
