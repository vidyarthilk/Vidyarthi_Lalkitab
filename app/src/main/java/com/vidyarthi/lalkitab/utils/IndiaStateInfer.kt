package com.vidyarthi.lalkitab.utils

/**
 * ભારત માટે લાટ/લોન પરથી રાજ્યનો મોટો અંદાજ (બોર્ડર નજીક ભૂલ શકાય).
 * ડ્રોપડાઉનમાં "શહેર, રાજ્ય, દેશ" દેખાડવા — DB માં [state] ન હોય ત્યારે.
 *
 * બોક્સનો ક્રમ મહત્વનો છે: પહેલો મેળ લાગે એ જ લેવાય (ઉદા. શ્રી ગંગાનગર માટે પંજાબ પહેલાં નહીં).
 */
object IndiaStateInfer {

    private data class Box(
        val state: String,
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double,
    )

    /**
     * પશ્ચિમથી પૂર્વ / ઉત્તરથી દક્ષિણ — ઓવરલેપ વિસ્તારોમાં પહેલા નજીકનું/વિશિષ્ટ બોક્સ પહેલાં.
     */
    private val boxes: List<Box> = listOf(
        Box("Andaman and Nicobar Islands", 6.45, 14.75, 92.0, 94.25),
        Box("Lakshadweep", 8.0, 12.5, 71.5, 74.5),
        Box("Delhi", 28.4, 28.9, 76.85, 77.35),
        Box("Chandigarh", 30.65, 30.78, 76.75, 76.82),
        Box("Puducherry", 11.85, 12.05, 79.75, 79.9),
        Box("Dadra and Nagar Haveli and Daman and Diu", 20.0, 20.85, 72.6, 73.2),
        Box("Goa", 14.75, 15.78, 73.55, 74.42),
        Box("Sikkim", 27.05, 28.15, 88.0, 88.95),
        Box("Arunachal Pradesh", 26.35, 29.55, 91.35, 97.45),
        Box("Nagaland", 25.15, 26.75, 93.15, 95.75),
        Box("Manipur", 23.75, 25.85, 92.85, 94.85),
        Box("Mizoram", 21.85, 24.65, 92.15, 93.55),
        Box("Tripura", 22.75, 24.65, 90.85, 92.35),
        Box("Meghalaya", 25.0, 26.35, 89.75, 92.85),
        Box("Assam", 24.05, 28.05, 89.45, 96.05),
        Box("Telangana", 15.75, 19.95, 77.15, 81.75),
        Box("Karnataka", 11.55, 18.5, 74.0, 78.65),
        Box("Tamil Nadu", 8.05, 13.55, 76.15, 80.35),
        Box("Andhra Pradesh", 12.45, 19.35, 76.65, 84.85),
        Box("Gujarat", 20.0, 24.95, 68.1, 74.65),
        Box("Maharashtra", 15.55, 22.05, 72.55, 80.95),
        Box("Kerala", 8.15, 12.92, 74.85, 77.45),
        Box("Odisha", 17.65, 22.65, 81.15, 87.55),
        Box("Chhattisgarh", 17.75, 24.15, 80.15, 84.55),
        Box("Jharkhand", 21.45, 25.45, 83.0, 88.0),
        Box("Bihar", 24.25, 27.75, 83.25, 88.55),
        Box("West Bengal", 21.45, 27.35, 85.75, 89.95),
        Box("Uttar Pradesh", 23.45, 31.45, 76.95, 84.75),
        Box("Rajasthan", 23.0, 32.5, 69.3, 78.4),
        Box("Madhya Pradesh", 21.45, 26.95, 74.0, 82.95),
        Box("Himachal Pradesh", 30.15, 33.45, 75.55, 79.05),
        Box("Uttarakhand", 28.55, 31.5, 77.45, 81.05),
        Box("Haryana", 27.45, 31.05, 74.45, 77.55),
        Box("Punjab", 29.45, 32.55, 73.45, 76.25),
        Box("Ladakh", 32.45, 35.65, 75.95, 79.85),
        Box("Jammu and Kashmir", 32.25, 37.25, 73.45, 80.35),
    )

    fun infer(latitude: Double, longitude: Double): String? {
        if (latitude < 6.2 || latitude > 37.6 || longitude < 67.8 || longitude > 98.6) return null
        for (b in boxes) {
            if (latitude in b.minLat..b.maxLat && longitude in b.minLon..b.maxLon) return b.state
        }
        return null
    }
}
