package com.yasminhosam.brandora.core.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Brandora color palette - Navy Blue theme (light) + Midnight Cyan (dark)
// ---------------------------------------------------------------------------

// ---- Light scheme ----------------------------------------------------------
val NavyPrimary = Color(0xFF1E3A8A)
val NavyOnPrimary = Color(0xFFFFFFFF)
val NavyPrimaryContainer = Color(0xFFE0E7F5)
val NavyOnPrimaryContainer = Color(0xFF0F2557)

val NavySecondary = Color(0xFF475569)
val NavyOnSecondary = Color(0xFFFFFFFF)
val NavySecondaryContainer = Color(0xFFE9EDF6)
val NavyOnSecondaryContainer = Color(0xFF1E293B)

// Tertiary is violet. It is also used for the "In Progress" order status.
val VioletTertiary = Color(0xFF5B21B6)
val VioletOnTertiary = Color(0xFFFFFFFF)
val VioletTertiaryContainer = Color(0xFFEDE9FE)
val VioletOnTertiaryContainer = Color(0xFF3B0F85)

val LightBackground = Color(0xFFF4F6FB)
val LightOnBackground = Color(0xFF0B1530)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0B1530)
val LightSurfaceVariant = Color(0xFFE9EDF6)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFF7A89A8)
val LightOutlineVariant = Color(0xFFE3E8F2)

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF9FAFD)
val LightSurfaceContainer = Color(0xFFF1F4FA)
val LightSurfaceContainerHigh = Color(0xFFEBEFF7)
val LightSurfaceContainerHighest = Color(0xFFE3E8F2)

val LightError = Color(0xFFB91C1C)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF7F1D1D)

val LightInverseSurface = Color(0xFF1B2540)
val LightInverseOnSurface = Color(0xFFEEF2FB)
val LightInversePrimary = Color(0xFF7DD3FC)

// ---- Dark scheme -----------------------------------------------------------
val CyanPrimary = Color(0xFF38BDF8)
val CyanOnPrimary = Color(0xFF04202F)
val CyanPrimaryContainer = Color(0xFF13304A)
val CyanOnPrimaryContainer = Color(0xFFBAE6FD)

val DarkSecondary = Color(0xFFA9B7D6)
val DarkOnSecondary = Color(0xFF0F1A33)
val DarkSecondaryContainer = Color(0xFF1F2D4D)
val DarkOnSecondaryContainer = Color(0xFFD9E2F7)

val VioletTertiaryDark = Color(0xFFC4B5FD)
val VioletOnTertiaryDark = Color(0xFF2A1A5E)
val VioletTertiaryContainerDark = Color(0xFF2A1F4D)
val VioletOnTertiaryContainerDark = Color(0xFFE4DCFF)

val DarkBackground = Color(0xFF0A1224)
val DarkOnBackground = Color(0xFFE8EEFB)
val DarkSurface = Color(0xFF111B33)
val DarkOnSurface = Color(0xFFE8EEFB)
val DarkSurfaceVariant = Color(0xFF1F2D4D)
val DarkOnSurfaceVariant = Color(0xFFA9B7D6)
val DarkOutline = Color(0xFF6A7BA3)
val DarkOutlineVariant = Color(0xFF1F2D4D)

val DarkSurfaceContainerLowest = Color(0xFF070E1C)
val DarkSurfaceContainerLow = Color(0xFF0D162B)
val DarkSurfaceContainer = Color(0xFF111B33)
val DarkSurfaceContainerHigh = Color(0xFF17233F)
val DarkSurfaceContainerHighest = Color(0xFF1F2D4D)

val DarkError = Color(0xFFF87171)
val DarkOnError = Color(0xFF3A0A0A)
val DarkErrorContainer = Color(0xFF3A1717)
val DarkOnErrorContainer = Color(0xFFFECACA)

val DarkInverseSurface = Color(0xFFE8EEFB)
val DarkInverseOnSurface = Color(0xFF111B33)
val DarkInversePrimary = Color(0xFF1E3A8A)

// ---- Status and Extended Theme Colors -------------------------------------
val TextMuted = LightOnSurfaceVariant
val Success = Color(0xFF047857)
val SuccessBackground = Color(0xFFD1FAE5)
val Warning = Color(0xFFB45309)
val WarningBackground = Color(0xFFFEF3C7)
val Info = Color(0xFF5B21B6)
val InfoBackground = Color(0xFFEDE9FE)
val Error = LightError
val ErrorBackground = Color(0xFFFEE2E2)

val DarkTextMuted = DarkOnSurfaceVariant
val DarkSuccess = Color(0xFF34D399)
val DarkSuccessBackground = Color(0xFF0F2E24)
val DarkWarning = Color(0xFFFBBF24)
val DarkWarningBackground = Color(0xFF3A2E10)
val DarkInfo = Color(0xFFC4B5FD)
val DarkInfoBackground = Color(0xFF2A1F4D)
val DarkErrorBackground = Color(0xFF3A1717)

// ---- Order status colors (content = text/icon, container = badge background)
// Pending = amber, Approved/Completed = green, Rejected/Cancelled = red,
// In Progress = violet (not blue, so it does not clash with the navy brand).
object StatusLight {
    val PendingContent = Warning
    val PendingContainer = WarningBackground
    val ApprovedContent = Success
    val ApprovedContainer = SuccessBackground
    val RejectedContent = Error
    val RejectedContainer = ErrorBackground
    val InProgressContent = Info
    val InProgressContainer = InfoBackground
}

object StatusDark {
    val PendingContent = DarkWarning
    val PendingContainer = DarkWarningBackground
    val ApprovedContent = DarkSuccess
    val ApprovedContainer = DarkSuccessBackground
    val RejectedContent = DarkError
    val RejectedContainer = DarkErrorBackground
    val InProgressContent = DarkInfo
    val InProgressContainer = DarkInfoBackground
}

// ---- Hero card (e.g. "Today's sales" on Home) ------------------------------
object HeroLight {
    val Container = Color(0xFF0F2557)
    val Content = Color(0xFFFFFFFF)
    val ContentVariant = Color(0xFFC7D2EE)
    val Chip = Color(0x24FFFFFF) // white at ~14%
}

object HeroDark {
    val Container = Color(0xFF16348A)
    val Content = Color(0xFFFFFFFF)
    val ContentVariant = Color(0xFFBCCDF5)
    val Chip = Color(0x21FFFFFF) // white at ~13%
}