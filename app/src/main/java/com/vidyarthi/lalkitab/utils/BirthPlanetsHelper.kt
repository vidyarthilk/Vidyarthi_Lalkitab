package com.vidyarthi.lalkitab.utils

import com.vidyarthi.lalkitab.data.KundliData

/** Vaar (weekday) lord and Lal Kitab janma-samay graha (kalak = 60 min; not Vedic hora). */
object BirthPlanetsHelper {

    fun vaarAndHoraPlanetKeys(k: KundliData): Pair<String, String> {
        val dayInfo = PanchangEngine.calculateDayInfo(k)
        val birthJD = TimeUtils.julianDayUtcForBirth(k)
        val vaar = PanchangEngine.weekdayDayLordKey(
            PanchangEngine.panchangCalendarDayOfWeek(k, birthJD, dayInfo.sunriseJD)
        )
        val samay = LalKitabSamayGrahaCalculator.planetKeyAtBirth(k)
        return vaar to samay
    }
}
