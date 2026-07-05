package com.vidyarthi.lalkitab.data

import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.utils.TimeUtils
import com.vidyarthi.lalkitab.utils.SwissEphManager
import swisseph.SweConst
import swisseph.SweDate

data class PanchangPakshaInfo(
    val paksha: String,
    val tithi: Int
)

fun calculatePanchangPakshaInfo(k: KundliData): PanchangPakshaInfo {

    // ✅ get Swiss Ephemeris instance from manager (architecture safe)
    val sw = SwissEphManager.get()

    val ut = TimeUtils.localToUT(k.hour, k.minute, k.timezone)
    val jd = SweDate(k.year, k.month, k.day, ut, SweDate.SE_GREG_CAL).julDay

    val sun = DoubleArray(6)
    val moon = DoubleArray(6)

    sw.swe_calc_ut(
        jd,
        SweConst.SE_SUN,
        SweConst.SEFLG_SWIEPH,
        sun,
        StringBuffer()
    )

    sw.swe_calc_ut(
        jd,
        SweConst.SE_MOON,
        SweConst.SEFLG_SWIEPH,
        moon,
        StringBuffer()
    )

    val diff = (moon[0] - sun[0] + 360.0) % 360.0
    val tithi = (diff / 12.0).toInt() + 1

    val paksha = if (tithi <= 15) "Shukla" else "Krishna"

    return PanchangPakshaInfo(
        paksha = paksha,
        tithi = tithi
    )
}
