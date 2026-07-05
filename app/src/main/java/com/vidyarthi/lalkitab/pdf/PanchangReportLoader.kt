package com.vidyarthi.lalkitab.pdf

import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.data.PanchangMonthInfo
import com.vidyarthi.lalkitab.data.PanchangPakshaInfo
import com.vidyarthi.lalkitab.utils.KundliEngine
import com.vidyarthi.lalkitab.utils.KundliHolder
import com.vidyarthi.lalkitab.utils.LalKitabDashaCalculator
import com.vidyarthi.lalkitab.utils.LalKitabSamayGrahaCalculator
import com.vidyarthi.lalkitab.utils.PanchangEngine
import com.vidyarthi.lalkitab.utils.PanchangUiState
import com.vidyarthi.lalkitab.utils.TimeUtils

/** Synchronous panchang load for PDF (same data as [com.vidyarthi.lalkitab.ui.panchang.PanchangViewModel]). */
object PanchangReportLoader {

    fun load(k: KundliData): PanchangUiState {
        val dayInfo = PanchangEngine.calculateDayInfo(k)
        val tithiInfo = PanchangEngine.calculateTithiInfo(k)
        val birthJD = TimeUtils.julianDayUtcForBirth(k)
        val nakshatraInfo = PanchangEngine.calculateNakshatraInfo(k)
        val yogaKaranaInfo = PanchangEngine.calculateYogaKaranaInfoAtJd(birthJD, k.timezone)
        val sunriseJD = dayInfo.sunriseJD
        val engineMonthInfo = PanchangEngine.calculateMonthInfo(
            jdSunriseToday = sunriseJD,
            timezone = k.timezone,
            k = k,
            tithiIndex = tithiInfo.uditaTithiIndex
        )
        val amavasyaDate = PanchangEngine.amavasyaLocalDateBeforeSunrise(sunriseJD, k.timezone)
        val monthInfo = PanchangMonthInfo(
            maasIndex = engineMonthInfo.maas,
            isAdhik = engineMonthInfo.isAdhik,
            amavasyaDate = amavasyaDate
        )
        val pakshaInfo = calculatePaksha(k)
        val vikramSamvat = PanchangEngine.calculateVikramSamvat(k, monthInfo, pakshaInfo)
        val birthdayPlanetKey = PanchangEngine.weekdayDayLordKey(
            PanchangEngine.panchangCalendarDayOfWeek(k, birthJD, dayInfo.sunriseJD)
        )
        val birthtimePlanetKey =
            LalKitabSamayGrahaCalculator.planetKeyAtBirthJd(k, birthJD, dayInfo)
        val janma = KundliEngine.calculate(k)
        var samayName = birthtimePlanetKey
        var samayG = janma.grahas.firstOrNull { it.name == samayName }
        if (samayG == null) {
            samayName = LalKitabDashaCalculator.MOON_NAME
            samayG = janma.grahas.firstOrNull { it.name == samayName }
        }
        val samayHouse = samayG?.house?.coerceIn(1, 12) ?: 1
        KundliHolder.dashaSamayGrahaName = samayName
        return PanchangUiState(
            isLoading = false,
            dayInfo = dayInfo,
            tithiInfo = tithiInfo,
            nakshatraInfo = nakshatraInfo,
            yogaKaranaInfo = yogaKaranaInfo,
            monthInfo = monthInfo,
            pakshaInfo = pakshaInfo,
            vikramSamvat = vikramSamvat,
            birthdayPlanetKey = birthdayPlanetKey,
            birthtimePlanetKey = birthtimePlanetKey,
            samayGrahaDashaName = samayName,
            samayGrahaJanmaHouse = samayHouse
        )
    }

    private fun calculatePaksha(k: KundliData): PanchangPakshaInfo {
        val riseUT = PanchangEngine.sunRiseSetSwiss(
            k.year, k.month, k.day,
            k.longitude, k.latitude,
            k.timezone,
            true
        )
        val phase = PanchangEngine.moonSunDiff(riseUT)
        val paksha = if (phase < 180) "Shukla" else "Krishna"
        val tithi = (phase / 12.0).toInt() + 1
        return PanchangPakshaInfo(paksha = paksha, tithi = tithi)
    }
}
