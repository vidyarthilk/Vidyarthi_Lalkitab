package com.vidyarthi.lalkitab.utils

/**
 * Default graha(s) for a rashi when the Moon-chart house is empty (Lal Kitab style).
 * Keys are Gujarati rashi names from [KundliEngine].
 */
object LalKitabSignLord {

    /** One or more internal graha names (Gujarati). Scorpio: Mars + Saturn; Virgo: Mercury + Ketu. */
    fun lordPlanetsGujarati(rashiGujarati: String): List<String> {
        return when (rashiGujarati.trim()) {
            "મેષ" -> listOf("મંગળ")
            "વૃષભ", "તુલા" -> listOf("શુક્ર")
            "મિથુન" -> listOf("બુધ")
            "કન્યા" -> listOf("બુધ", "કેતુ")
            "કર્ક" -> listOf("ચંદ્ર")
            "સિંહ" -> listOf("સૂર્ય")
            "વૃશ્ચિક" -> listOf("મંગળ", "શનિ")
            "ધનુ", "મીન" -> listOf("ગુરુ")
            "મકર", "કુંભ" -> listOf("શનિ")
            else -> listOf("ગુરુ")
        }
    }
}
