package com.yasminhosam.brandora.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = OnBrandPrimaryContainer,
    inversePrimary = DarkPrimary,

    secondary = TextSecondary,
    onSecondary = Color.White,
    secondaryContainer = BorderStrong,
    onSecondaryContainer = TextPrimary,

    tertiary = Info,
    onTertiary = Color.White,
    tertiaryContainer = InfoBackground,
    onTertiaryContainer = OnInfoContainer,

    background = Background,
    onBackground = TextPrimary,

    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = TextSecondary,
    surfaceTint = BrandPrimary,
    inverseSurface = TextPrimary,
    inverseOnSurface = Background,

    surfaceContainerLowest = Surface,
    surfaceContainerLow = Background,
    surfaceContainer = SurfaceSoft,
    surfaceContainerHigh = BorderStrong,
    surfaceContainerHighest = BorderStrong,
    surfaceDim = BorderStrong,
    surfaceBright = Surface,

    outline = BorderStrong,
    outlineVariant = Border,

    error = Error,
    onError = Color.White,
    errorContainer = ErrorBackground,
    onErrorContainer = OnErrorContainer,

    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = BrandPrimary,

    secondary = DarkTextSecondary,
    onSecondary = DarkSurfaceVariant,
    secondaryContainer = DarkBorderStrong,
    onSecondaryContainer = DarkTextPrimary,

    tertiary = DarkInfo,
    onTertiary = Color(0xFF172554),
    tertiaryContainer = DarkInfoBackground,
    onTertiaryContainer = DarkOnInfoContainer,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = DarkPrimary,
    inverseSurface = DarkTextPrimary,
    inverseOnSurface = DarkSurface,

    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = Color(0xFF131C2E),
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkSurfaceHigh,
    surfaceDim = DarkBackground,
    surfaceBright = DarkSurfaceHigh,

    outline = DarkBorderStrong,
    outlineVariant = DarkBorder,

    error = DarkError,
    onError = Color(0xFF450A0A),
    errorContainer = DarkErrorBackground,
    onErrorContainer = DarkOnErrorContainer,

    scrim = Color.Black
)

@Composable
fun BrandoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}
