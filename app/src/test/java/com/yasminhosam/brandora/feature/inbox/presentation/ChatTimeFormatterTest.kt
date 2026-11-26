package com.yasminhosam.brandora.feature.inbox.presentation

import com.yasminhosam.brandora.feature.inbox.presentation.util.ChatTimeFormatter
import com.yasminhosam.brandora.feature.inbox.presentation.util.DayLabel
import com.yasminhosam.brandora.feature.inbox.presentation.util.appendDistinct
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTimeFormatterTest {
    private val zone = ZoneId.of("Africa/Cairo")
    private val now = Instant.parse("2026-11-21T12:00:00Z")

    @Test
    fun today_yesterday_andOlder() {
        assertTrue(ChatTimeFormatter.inboxLabel(Instant.parse("2026-11-21T08:00:00Z"), now, zone, Locale.US) is DayLabel.Time)
        assertEquals(DayLabel.Yesterday, ChatTimeFormatter.inboxLabel(Instant.parse("2026-11-20T08:00:00Z"), now, zone, Locale.US))
        assertTrue(ChatTimeFormatter.inboxLabel(Instant.parse("2026-11-01T08:00:00Z"), now, zone, Locale.US) is DayLabel.Date)
    }

    @Test
    fun dayBoundaryUsesTheLocalZone() {
        // 23:30 UTC on the 20th is already the 21st in Cairo (UTC+2).
        assertEquals(DayLabel.Today, ChatTimeFormatter.daySeparator(Instant.parse("2026-11-20T23:30:00Z"), now, zone, Locale.US))
    }

    @Test
    fun appendDistinct_dropsRowsThatShiftedBetweenPages() {
        val result = listOf("a", "b").appendDistinct(listOf("b", "c")) { it }
        assertEquals(listOf("a", "b", "c"), result)
    }
}
