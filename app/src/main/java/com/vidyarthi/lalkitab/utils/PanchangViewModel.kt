package com.vidyarthi.lalkitab.ui.panchang

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.*
import com.vidyarthi.lalkitab.utils.PanchangEngine
import com.vidyarthi.lalkitab.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.vidyarthi.lalkitab.utils.PanchangUiState
import com.vidyarthi.lalkitab.utils.KundliEngine
import com.vidyarthi.lalkitab.utils.KundliHolder
import com.vidyarthi.lalkitab.utils.LalKitabDashaCalculator
import com.vidyarthi.lalkitab.utils.LalKitabSamayGrahaCalculator

class PanchangViewModel(application: Application) : AndroidViewModel(application) {
    private data class ResultData(
        val dayInfo: PanchangDayInfo,
        val tithiInfo: PanchangTithiInfo,
        val nakshatraInfo: PanchangNakshatraInfo,
        val yogaKaranaInfo: PanchangYogaKaranaInfo,
        val monthInfo: PanchangMonthInfo,
        val pakshaInfo: PanchangPakshaInfo,
        val vikramSamvat: Int,
        val birthdayPlanetKey: String,
        val birthtimePlanetKey: String,
        val samayGrahaDashaName: String,
        val samayGrahaJanmaHouse: Int
    )
    private val _uiState = MutableStateFlow(PanchangUiState())
    val uiState: StateFlow<PanchangUiState> = _uiState

    fun loadPanchang(k: KundliData) {

        viewModelScope.launch {
            try {
                _uiState.update { state: PanchangUiState ->
                    state.copy(isLoading = true, error = null)
                }

                val result: ResultData = withTimeout(15000) {
                    withContext(Dispatchers.IO) {

                        // ---- Day Info (sunrise-based) ----
                        val dayInfo = PanchangEngine.calculateDayInfo(k)

                        // ---- Tithi (day-based) ----
                        val tithiInfo = PanchangEngine.calculateTithiInfo(k)

                        val birthJD = TimeUtils.julianDayUtcForBirth(k)

                        // ---- Nakshatra / Yoga / Karana (birth-time) ----
                        val nakshatraInfo = PanchangEngine.calculateNakshatraInfo(k)
                        val yogaKaranaInfo = PanchangEngine.calculateYogaKaranaInfoAtJd(birthJD, k.timezone)

                        // ---- Sunrise JD (for month / paksha) ----
                        val sunriseJD = dayInfo.sunriseJD

                        // ---- Month Info ----
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

                        // ---- Paksha ----
                        val pakshaInfo = calculatePanchangPakshaInfo(k)

                        // ---- Vikram Samvat ----
                        val vikramSamvat = PanchangEngine.calculateVikramSamvat(
                            k,
                            monthInfo,
                            pakshaInfo
                        )

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

                        ResultData(
                            dayInfo,
                            tithiInfo,
                            nakshatraInfo,
                            yogaKaranaInfo,
                            monthInfo,
                            pakshaInfo,
                            vikramSamvat,
                            birthdayPlanetKey,
                            birthtimePlanetKey,
                            samayGrahaDashaName = samayName,
                            samayGrahaJanmaHouse = samayHouse
                        )
                    }
                }
                KundliHolder.dashaSamayGrahaName = result.samayGrahaDashaName

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dayInfo = result.dayInfo,
                        tithiInfo = result.tithiInfo,
                        nakshatraInfo = result.nakshatraInfo,
                        yogaKaranaInfo = result.yogaKaranaInfo,
                        monthInfo = result.monthInfo,
                        pakshaInfo = result.pakshaInfo,
                        vikramSamvat = result.vikramSamvat,
                        birthdayPlanetKey = result.birthdayPlanetKey,
                        birthtimePlanetKey = result.birthtimePlanetKey,
                        samayGrahaDashaName = result.samayGrahaDashaName,
                        samayGrahaJanmaHouse = result.samayGrahaJanmaHouse
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: getApplication<Application>().getString(R.string.error_unknown)
                    )
                }
            }
            }
    }
    fun calculatePanchangPakshaInfo(k: KundliData): PanchangPakshaInfo {
        val riseUT = PanchangEngine.sunRiseSetSwiss(
            k.year, k.month, k.day,
            k.longitude, k.latitude,
            k.timezone,
            true
        )

        val setUT = PanchangEngine.sunRiseSetSwiss(
            k.year, k.month, k.day,
            k.longitude, k.latitude,
            k.timezone,
            false
        )

        val phase = PanchangEngine.moonSunDiff(riseUT)

        val paksha = if (phase < 180) "Shukla" else "Krishna"

        val tithi = (phase / 12.0).toInt() + 1

        return PanchangPakshaInfo(
            paksha = paksha,
            tithi = tithi
        )
    }
}