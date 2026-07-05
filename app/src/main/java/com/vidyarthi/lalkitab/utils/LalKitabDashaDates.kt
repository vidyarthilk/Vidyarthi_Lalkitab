package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.data.LalKitabDashaSegment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LalKitabDashaDates {

    private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun dob(k: KundliData): LocalDateTime =
        LocalDateTime.of(k.year, k.month, k.day, k.hour, k.minute)

    /** Start instant of life-year [seg.startYear] (year 1 begins at birth). */
    fun segmentStart(k: KundliData, seg: LalKitabDashaSegment): LocalDateTime =
        dob(k).plusYears((seg.startYear - 1).toLong())

    /**
     * End boundary displayed like the annual chart: same calendar day after [seg.endYear] full years from birth.
     */
    fun segmentEndDisplay(k: KundliData, seg: LalKitabDashaSegment): LocalDateTime =
        dob(k).plusYears(seg.endYear.toLong())

    fun formatSegmentRange(k: KundliData, seg: LalKitabDashaSegment): Pair<String, String> {
        val a = segmentStart(k, seg).format(dayFmt)
        val b = segmentEndDisplay(k, seg).format(dayFmt)
        return a to b
    }

    /** Approximate calendar instant for fractional life-year (1.0 = birth). */
    fun atLifeYearApprox(k: KundliData, lifeYear: Double): LocalDateTime {
        val base = dob(k)
        val secs = ((lifeYear - 1.0) * 365.2425 * 86400.0).toLong()
        return base.plusSeconds(secs)
    }

    fun formatAntarRange(k: KundliData, startFrac: Double, endFrac: Double): Pair<String, String> {
        val a = atLifeYearApprox(k, startFrac).format(dayFmt)
        val b = atLifeYearApprox(k, endFrac).format(dayFmt)
        return a to b
    }
}
