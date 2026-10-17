package com.yasminhosam.brandora.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Material 3 color schemes
// ---------------------------------------------------------------------------

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = NavyOnPrimary,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = NavyOnPrimaryContainer,
    inversePrimary = LightInversePrimary,
    secondary = NavySecondary,
    onSecondary = NavyOnSecondary,
    secondaryContainer = NavySecondaryContainer,
    onSecondaryContainer = NavyOnSecondaryContainer,
    tertiary = VioletTertiary,
    onTertiary = VioletOnTertiary,
    tertiaryContainer = VioletTertiaryContainer,
    onTertiaryContainer = VioletOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = NavyPrimary,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = CyanOnPrimary,
    primaryContainer = CyanPrimaryContainer,
    onPrimaryContainer = CyanOnPrimaryContainer,
    inversePrimary = DarkInversePrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = VioletTertiaryDark,
    onTertiary = VioletOnTertiaryDark,
    tertiaryContainer = VioletTertiaryContainerDark,
    onTertiaryContainer = VioletOnTertiaryContainerDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = CyanPrimary,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

// ---------------------------------------------------------------------------
// Extra brand colors that Material 3 has no slot for
// ---------------------------------------------------------------------------

/** A foreground/background pair, e.g. for a status badge. */
@Immutable
data class ColorPair(val content: Color, val container: Color)

@Immutable
data class StatusColors(
    val pending: ColorPair,
    val approved: ColorPair,
    val rejected: ColorPair,
    val inProgress: ColorPair,
)

@Immutable
data class HeroColors(
    val container: Color,
    val content: Color,
    val contentVariant: Color,
    val chip: Color,
)

@Immutable
data class BrandoraExtendedColors(
    val status: StatusColors,
    val hero: HeroColors,
)

private val LightBrandoraExtendedColors = BrandoraExtendedColors(
    status = StatusColors(
        pending = ColorPair(StatusLight.PendingContent, StatusLight.PendingContainer),
        approved = ColorPair(StatusLight.ApprovedContent, StatusLight.ApprovedContainer),
        rejected = ColorPair(StatusLight.RejectedContent, StatusLight.RejectedContainer),
        inProgress = ColorPair(StatusLight.InProgressContent, StatusLight.InProgressContainer),
    ),
    hero = HeroColors(
        container = HeroLight.Container,
        content = HeroLight.Content,
        contentVariant = HeroLight.ContentVariant,
        chip = HeroLight.Chip,
    ),
)

private val DarkBrandoraExtendedColors = BrandoraExtendedColors(
    status = StatusColors(
        pending = ColorPair(StatusDark.PendingContent, StatusDark.PendingContainer),
        approved = ColorPair(StatusDark.ApprovedContent, StatusDark.ApprovedContainer),
        rejected = ColorPair(StatusDark.RejectedContent, StatusDark.RejectedContainer),
        inProgress = ColorPair(StatusDark.InProgressContent, StatusDark.InProgressContainer),
    ),
    hero = HeroColors(
        container = HeroDark.Container,
        content = HeroDark.Content,
        contentVariant = HeroDark.ContentVariant,
        chip = HeroDark.Chip,
    ),
)

private val LocalBrandoraExtendedColors = staticCompositionLocalOf { LightBrandoraExtendedColors }

/**
 * Access the extra brand colors:
 *
 *     val badge = BrandoraTheme.extended.status.pending
 *     Text("Pending", color = badge.content, modifier = Modifier.background(badge.container))
 */
object BrandoraTheme {
    val extended: BrandoraExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBrandoraExtendedColors.current
}

// ---------------------------------------------------------------------------
// Theme entry point
// ---------------------------------------------------------------------------

/**
 * Brandora app theme.
 *
 * Light mode uses the Navy Blue palette, dark mode uses Midnight Cyan.
 * Dynamic color (Material You) is not used so the brand colors stay consistent.
 *
 * `Typography` comes from the Type.kt generated by the Android Studio template.
 */
@Composable
fun BrandoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkBrandoraExtendedColors else LightBrandoraExtendedColors
    val statusStyleExtendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalBrandoraExtendedColors provides extendedColors,
        LocalExtendedColors provides statusStyleExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}