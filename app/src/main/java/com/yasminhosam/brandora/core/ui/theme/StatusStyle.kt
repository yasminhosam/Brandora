package com.yasminhosam.brandora.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class StatusStyle(val content: Color, val container: Color)
data class ExtendedColors(
    val textMuted: Color,
    val success: StatusStyle,
    val warning: StatusStyle,
    val info: StatusStyle,
    val danger: StatusStyle
) {
    val pending get() = warning
    val approved get() = success      // also Completed
    val inProgress get() = info
    val cancelled get() = danger      // also Rejected
}

val LightExtendedColors = ExtendedColors(
    textMuted = TextMuted,
    success = StatusStyle(Success, SuccessBackground),
    warning = StatusStyle(Warning, WarningBackground),
    info = StatusStyle(Info, InfoBackground),
    danger = StatusStyle(Error, ErrorBackground)
)

val DarkExtendedColors = ExtendedColors(
    textMuted = DarkTextMuted,
    success = StatusStyle(DarkSuccess, DarkSuccessBackground),
    warning = StatusStyle(DarkWarning, DarkWarningBackground),
    info = StatusStyle(DarkInfo, DarkInfoBackground),
    danger = StatusStyle(DarkError, DarkErrorBackground)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extended: ExtendedColors
    @Composable get() = LocalExtendedColors.current