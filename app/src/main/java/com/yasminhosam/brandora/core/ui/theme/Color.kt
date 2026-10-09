package com.yasminhosam.brandora.core.ui.theme

import androidx.compose.ui.graphics.Color

// ───────── LIGHT ─────────
val BrandPrimary = Color(0xFF7563A8)               // mauve: buttons, selected items
val BrandPrimaryDark = Color(0xFF5E4F8C)           // derived: pressed state
val BrandPrimaryContainer = Color(0xFFEEE9F6)      // light lavender: selected background
val OnBrandPrimaryContainer = Color(0xFF2F2650)    // derived

val Accent = Color(0xFFF2C6B4)                     // soft peach: extra touch
val OnAccent = Color(0xFF5A3322)                   // derived

val Background = Color(0xFFFAF9FC)                 // warm white
val Surface = Color(0xFFFFFFFF)                    // cards
val SurfaceSoft = Color(0xFFF4F1F9)                // derived

val TextPrimary = Color(0xFF292436)
val TextSecondary = Color(0xFF716B7D)
val TextMuted = Color(0xFFA39EAD)                  // derived

val Border = Color(0xFFEFECF4)                     // derived (lighter divider)
val BorderStrong = Color(0xFFE5E1EC)

val Success = Color(0xFF287A59)
val SuccessBackground = Color(0xFFDDF0E7)          // derived
val Error = Color(0xFFBA414D)
val ErrorBackground = Color(0xFFF8E1E3)            // derived
val OnErrorContainer = Color(0xFF7A2530)           // derived
val Warning = Color(0xFF946200)
val WarningBackground = Color(0xFFF8EBCF)          // derived

// "In Progress" blue (not in the palette, tuned to sit well with the mauve)
val Info = Color(0xFF3F5FA8)
val InfoBackground = Color(0xFFE1E8F6)
val OnInfoContainer = Color(0xFF1F3366)

// ───────── DARK (derived from the same mauve family) ─────────
val DarkPrimary = Color(0xFFB7A8E0)
val DarkOnPrimary = Color(0xFF2A2145)
val DarkPrimaryContainer = Color(0xFF4A3D78)
val DarkOnPrimaryContainer = Color(0xFFEEE9F6)

val DarkAccent = Color(0xFFF2C6B4)
val DarkOnAccent = Color(0xFF4A2A1B)

val DarkBackground = Color(0xFF14111B)
val DarkSurface = Color(0xFF1B1824)
val DarkSurfaceVariant = Color(0xFF262232)
val DarkSurfaceHigh = Color(0xFF312C3F)

val DarkTextPrimary = Color(0xFFF2EFF7)
val DarkTextSecondary = Color(0xFFC9C3D4)
val DarkTextMuted = Color(0xFF8F889D)

val DarkBorder = Color(0xFF262232)
val DarkBorderStrong = Color(0xFF3A3548)

val DarkSuccess = Color(0xFF5FCB9E)
val DarkSuccessBackground = Color(0xFF0F2E22)
val DarkError = Color(0xFFF08A93)
val DarkErrorBackground = Color(0xFF3A171B)
val DarkOnErrorContainer = Color(0xFFF9C9CD)
val DarkWarning = Color(0xFFE8B54A)
val DarkWarningBackground = Color(0xFF3A2A0B)
val DarkInfo = Color(0xFF8FA9E8)
val DarkInfoBackground = Color(0xFF17223F)
val DarkOnInfoContainer = Color(0xFFDCE4F8)