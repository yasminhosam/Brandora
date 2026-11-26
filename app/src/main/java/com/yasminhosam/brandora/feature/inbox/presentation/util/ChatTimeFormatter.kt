package com.yasminhosam.brandora.feature.inbox.presentation.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** What to show for a timestamp; the UI resolves [Yesterday] to a localized string. */
sealed interface DayLabel {
    data class Time(val text: String) : DayLabel
    data object Today : DayLabel
    data object Yesterday : DayLabel
    data class Date(val text: String) : DayLabel
}

/** Pure functions (clock, zone and locale are parameters) so they are unit-testable. */
object ChatTimeFormatter {

    fun clockTime(instant: Instant, zone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .format(instant.atZone(zone))

    /** Inbox row: today -> time, yesterday -> label, otherwise a short date. */
    fun inboxLabel(instant: Instant, now: Instant, zone: ZoneId, locale: Locale): DayLabel {
        val day = instant.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        return when (day) {
            today -> DayLabel.Time(clockTime(instant, zone, locale))
            today.minusDays(1) -> DayLabel.Yesterday
            else -> DayLabel.Date(dateText(instant, zone, locale))
        }
    }

    /** Chat day separator: today / yesterday / full date. */
    fun daySeparator(instant: Instant, now: Instant, zone: ZoneId, locale: Locale): DayLabel {
        val day = instant.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        return when (day) {
            today -> DayLabel.Today
            today.minusDays(1) -> DayLabel.Yesterday
            else -> DayLabel.Date(dateText(instant, zone, locale))
        }
    }

    fun isSameDay(a: Instant, b: Instant, zone: ZoneId): Boolean =
        a.atZone(zone).toLocalDate() == b.atZone(zone).toLocalDate()

    private fun dateText(instant: Instant, zone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .format(instant.atZone(zone))
}
