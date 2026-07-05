package com.vidyarthi.lalkitab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.vidyarthi.lalkitab.R

/** Same weights as res/font/kalam_family.xml — used for Panchang Compose and any M3 text. */
val KalamFontFamily: FontFamily = FontFamily(
    Font(R.font.kalam_light, FontWeight.Light),
    Font(R.font.kalam_regular, FontWeight.Normal),
    Font(R.font.kalam_bold, FontWeight.Bold),
)

private val base: Typography = Typography()

val Typography: Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = KalamFontFamily),
    displayMedium = base.displayMedium.copy(fontFamily = KalamFontFamily),
    displaySmall = base.displaySmall.copy(fontFamily = KalamFontFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = KalamFontFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = KalamFontFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = KalamFontFamily),
    titleLarge = base.titleLarge.copy(fontFamily = KalamFontFamily),
    titleMedium = base.titleMedium.copy(fontFamily = KalamFontFamily),
    titleSmall = base.titleSmall.copy(fontFamily = KalamFontFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = KalamFontFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = KalamFontFamily),
    bodySmall = base.bodySmall.copy(fontFamily = KalamFontFamily),
    labelLarge = base.labelLarge.copy(fontFamily = KalamFontFamily),
    labelMedium = base.labelMedium.copy(fontFamily = KalamFontFamily),
    labelSmall = base.labelSmall.copy(fontFamily = KalamFontFamily),
)
