package com.vidyarthi.lalkitab.utils



import com.vidyarthi.lalkitab.data.KundliChart

import com.vidyarthi.lalkitab.data.KundliData

import com.vidyarthi.lalkitab.data.LalKitabDashaSegment



object VarshfalRajaVazirHelper {

    /** Fixed sign order for display index 1…12 (Mesh … Meen). */
    val GUJARATI_SIGNS_IN_ORDER: List<String> = listOf(
        "મેષ", "વૃષભ", "મિથુન", "કર્ક", "સિંહ", "કન્યા",
        "તુલા", "વૃશ્ચિક", "ધનુ", "મકર", "કુંભ", "મીન"
    )

    /** 1-based rashi index from Gujarati sign name (for UI “રાશિ 5”). */
    fun rashiDisplayNumber(gujaratiSign: String): Int {
        val i = GUJARATI_SIGNS_IN_ORDER.indexOf(gujaratiSign.trim())
        return if (i < 0) 1 else i + 1
    }

    private val RASHI_BY_HOUSE1_MESH = arrayOf(

        "મેષ", "વૃષભ", "મિથુન", "કર્ક", "સિંહ", "કન્યા",

        "તુલા", "વૃશ્ચિક", "ધનુ", "મકર", "કુંભ", "મીન"

    )



    /**
     * Lal Kitab vazir rule: rashi for a khana is always counted with Mesh fixed at khana 1.
     * So khana 8 is always Vrushchik (not lagna-shifted), for both janma and chandra paths.
     */
    fun signAtHouseJanmaChart(janma: KundliChart, house: Int): String {
        val h = house.coerceIn(1, 12)
        return RASHI_BY_HOUSE1_MESH[h - 1]
    }



    fun computeCurrentLifeYear(k: KundliData): Int {

        val birth = java.time.LocalDate.of(k.year, k.month, k.day)

        val today = java.time.LocalDate.now()

        val years = java.time.Period.between(birth, today).years

        return (years + 1).coerceIn(1, 120)

    }



    /** Mahadasha segment; [fromChandra] uses Moon-chart house for samay graha (same 35-yr rules). */

    fun mahadashaForLifeYear(k: KundliData, lifeYear: Int, fromChandra: Boolean): LalKitabDashaSegment? {

        val result = LalKitabDashaCalculator.compute(k, useChandraChartForHouse = fromChandra) ?: return null

        val y = lifeYear.coerceIn(1, LalKitabDashaCalculator.DEFAULT_MAX_YEAR)

        return result.segments.firstOrNull { y in it.startYear..it.endYear }

    }



    data class VazirResult(

        val khana: Int,

        val signGujarati: String,

        val grahaKeys: List<String>,

        val usedSignLord: Boolean

    )



    /** Vazir from planets in [khana] for the chosen chart. */

    fun computeVazir(k: KundliData, lifeYear: Int, fromChandra: Boolean): VazirResult {

        val khana = RashifalKhanaAgeTable.khanaForLifeYear(lifeYear)

        return if (fromChandra) {

            val chandra = KundliEngine.calculateChandraKundli(k)

            val sign = signAtHouseJanmaChart(chandra, khana)

            val inHouse = chandra.grahas

                .filter { it.house == khana }

                .map { it.name }

                .distinct()

            if (inHouse.isNotEmpty()) {

                VazirResult(khana, sign, inHouse, usedSignLord = false)

            } else {

                val lords = LalKitabSignLord.lordPlanetsGujarati(sign)

                VazirResult(khana, sign, lords, usedSignLord = true)

            }

        } else {

            val janma = KundliEngine.calculate(k)

            val sign = signAtHouseJanmaChart(janma, khana)

            val inHouse = janma.grahas

                .filter { it.house == khana }

                .map { it.name }

                .distinct()

            if (inHouse.isNotEmpty()) {

                VazirResult(khana, sign, inHouse, usedSignLord = false)

            } else {

                val lords = LalKitabSignLord.lordPlanetsGujarati(sign)

                VazirResult(khana, sign, lords, usedSignLord = true)

            }

        }

    }

}


