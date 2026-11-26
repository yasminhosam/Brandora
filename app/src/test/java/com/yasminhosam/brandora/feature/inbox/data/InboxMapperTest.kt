package com.yasminhosam.brandora.feature.inbox.data

import com.yasminhosam.brandora.feature.inbox.data.mapper.toDomain
import com.yasminhosam.brandora.feature.inbox.data.mapper.toInstantOrNull
import com.yasminhosam.brandora.feature.inbox.data.remote.ConversationsResponseDto
import com.yasminhosam.brandora.feature.inbox.data.remote.MessagesResponseDto
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.DeliveryStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.MessageType
import com.yasminhosam.brandora.feature.inbox.domain.model.SenderType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxMapperTest {

    // Same settings as the app's NetworkModule.
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    @Test
    fun conversationsList_parsesAndMaps() {
        val dto = json.decodeFromString<ConversationsResponseDto>(
            """
            {"pagination":{"limit":20,"offset":0,"totalCount":2,"hasMore":true,"nextOffset":20},
             "items":[{"id":"a","businessId":"b","customer":{"id":"c","phone":"201000000002","name":null},
               "status":"CLOSED","aiEnabled":false,"handoffReason":"CUSTOMER_REQUESTED_HUMAN",
               "lastMessageAt":"2026-11-08T15:42:45.616Z",
               "lastMessage":{"id":"m","senderType":"AI","messageType":"IMAGE","status":"SENT","content":null,
                 "aiMeta":{"x":1},"createdAt":"2026-11-08T15:42:45.616Z"}}]}
            """.trimIndent()
        ).toDomain()

        assertEquals(2, dto.totalCount)
        assertEquals(20, dto.nextOffset)
        val c = dto.items.single()
        assertEquals(ConversationStatus.CLOSED, c.status)
        assertTrue(c.needsOwnerReply)
        assertNull(c.customer.name)
        assertEquals("+201000000002", c.customer.title)
        assertEquals(SenderType.AI, c.lastMessage?.sender)
        assertEquals(MessageType.IMAGE, c.lastMessage?.type)
        assertFalse(c.lastMessage!!.hasText)
    }

    @Test
    fun unknownEnumValues_degradeInsteadOfCrashing() {
        val page = json.decodeFromString<MessagesResponseDto>(
            """{"pagination":{"hasMore":false},"items":[{"id":"m","senderType":"ROBOT","messageType":"STICKER","status":"WEIRD","content":"hi"}]}"""
        ).toDomain()
        val m = page.items.single()
        assertEquals(SenderType.UNKNOWN, m.sender)
        assertEquals(MessageType.OTHER, m.type)
        assertEquals(DeliveryStatus.UNKNOWN, m.status)
        assertNull(m.createdAt)
        assertNull(page.nextOffset)
    }

    @Test
    fun nextOffset_isNullWhenHasMoreIsFalse() {
        val page = json.decodeFromString<MessagesResponseDto>(
            """{"pagination":{"totalCount":1,"hasMore":false,"nextOffset":20},"items":[]}"""
        ).toDomain()
        assertNull(page.nextOffset)
    }

    @Test
    fun badTimestamp_becomesNull() {
        assertNull("not-a-date".toInstantOrNull())
        assertNull((null as String?).toInstantOrNull())
    }
}
