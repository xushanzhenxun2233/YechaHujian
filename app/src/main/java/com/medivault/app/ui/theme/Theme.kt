package com.medivault.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MediTeal = Color(0xFF0F766E)
val MediTealLight = Color(0xFFCCFBF1)
val MediTealDark = Color(0xFF115E59)
val MediBlue = Color(0xFF0369A1)
val MediAmber = Color(0xFFD97706)
val MediAmberBg = Color(0xFFFEF3C7)
val MediRose = Color(0xFFBE123C)
val MediSurfaceBg = Color(0xFFF4F7F6)
val MediCardBg = Color(0xFFFFFFFF)
val MediTextPrimary = Color(0xFF0F172A)
val MediTextSecondary = Color(0xFF475569)

private val MediLightColorScheme = lightColorScheme(
    primary = MediTeal,
    onPrimary = Color.White,
    primaryContainer = MediTealLight,
    onPrimaryContainer = MediTealDark,
    secondary = MediBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = MediAmber,
    onTertiary = Color.White,
    tertiaryContainer = MediAmberBg,
    onTertiaryContainer = Color(0xFF78350F),
    background = MediSurfaceBg,
    onBackground = MediTextPrimary,
    surface = MediCardBg,
    onSurface = MediTextPrimary,
    surfaceVariant = Color(0xFFE2ECEA),
    onSurfaceVariant = MediTextSecondary,
    error = MediRose,
    onError = Color.White
)

@Composable
fun MediVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MediLightColorScheme,
        content = content
    )
}
