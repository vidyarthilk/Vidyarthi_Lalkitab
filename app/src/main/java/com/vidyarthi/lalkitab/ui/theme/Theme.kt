package com.vidyarthi.lalkitab.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF9800),
    secondary = androidx.compose.ui.graphics.Color(0xFFFF9800),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFF9800),
    background = androidx.compose.ui.graphics.Color.Transparent,
    surface = androidx.compose.ui.graphics.Color.Transparent,
    outline = androidx.compose.ui.graphics.Color(0xFFFF9800),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFFF9800),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF8B0000),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF8B0000),
    onBackground = androidx.compose.ui.graphics.Color(0xFF8B0000),
    onSurface = androidx.compose.ui.graphics.Color(0xFF8B0000)
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF9800),
    secondary = androidx.compose.ui.graphics.Color(0xFFFF9800),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFF9800),
    background = androidx.compose.ui.graphics.Color.Transparent,
    surface = androidx.compose.ui.graphics.Color.Transparent,
    outline = androidx.compose.ui.graphics.Color(0xFFFF9800),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFFF9800),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF8B0000),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF8B0000),
    onBackground = androidx.compose.ui.graphics.Color(0xFF8B0000),
    onSurface = androidx.compose.ui.graphics.Color(0xFF8B0000)
)

@Composable
fun Vidyarthi_LalkitabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
