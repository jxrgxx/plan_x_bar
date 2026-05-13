package com.los_jorges.plan_bar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark Luxury ──────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = Platinum40,
    onPrimary = DarkBase,
    primaryContainer = DarkSurface3,
    onPrimaryContainer = Platinum80,

    secondary = Warm40,
    onSecondary = DarkBase,
    secondaryContainer = DarkSurface2,
    onSecondaryContainer = Warm80,

    tertiary = Green40,
    onTertiary = DarkBase,
    tertiaryContainer = Green10,
    onTertiaryContainer = Green80,

    background = DarkBase,
    onBackground = WarmWhite,
    surface = DarkSurface1,
    onSurface = WarmWhite,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = WarmGray,
    outline = WarmMuted,
    outlineVariant = DarkSurface3,

    error = Color(0xFFC0574A),
    onError = DarkBase,
    errorContainer = Color(0xFF3D1512),
    onErrorContainer = Color(0xFFFFADA6),

    inverseSurface = WarmWhite,
    inverseOnSurface = DarkBase,
    inversePrimary = Platinum30,

    scrim = Color(0x99000000),
)

// ── Light (fallback) ─────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = Platinum30,
    onPrimary = Color.White,
    primaryContainer = Platinum90,
    onPrimaryContainer = Platinum10,

    secondary = Warm40,
    onSecondary = Color.White,
    secondaryContainer = Warm90,
    onSecondaryContainer = Warm30,

    tertiary = Green40,
    onTertiary = Color.White,
    tertiaryContainer = Green90,
    onTertiaryContainer = Green30,

    background = LightBg,
    onBackground = DarkOnLight,
    surface = LightBg,
    onSurface = DarkOnLight,
    surfaceVariant = LightSurf,
    onSurfaceVariant = Warm30,
    outline = Warm40,
    outlineVariant = Warm80,
)

@Composable
fun Plan_BarTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
