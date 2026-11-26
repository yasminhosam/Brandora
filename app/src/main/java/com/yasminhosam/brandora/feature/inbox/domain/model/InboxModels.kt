package com.yasminhosam.brandora.feature.inbox.domain.model

import com.yasminhosam.brandora.core.network.ConversationStatusValues
import com.yasminhosam.brandora.core.network.MessageStatusValues
import com.yasminhosam.brandora.core.network.MessageTypeValues
import com.yasminhosam.brandora.core.network.SenderTypeValues
import java.time.Instant

enum class ConversationStatus {
    OPEN, CLOSED, UNKNOWN;

    companion object {
        /** Unknown or missing values become [UNKNOWN] so a new backend status degrades gracefully. */
        fun from(raw: String?): ConversationStatus = when (raw) {
            ConversationStatusValues.OPEN -> OPEN
            ConversationStatusValues.CLOSED -> CLOSED
            else -> UNKNOWN
        }
    }
}

enum class SenderType {
    CUSTOMER, AI, OWNER, UNKNOWN;

    companion object {
        fun from(raw: String?): SenderType = when (raw) {
            SenderTypeValues.CUSTOMER -> CUSTOMER
            SenderTypeValues.AI -> AI
            SenderTypeValues.OWNER -> OWNER
            else -> UNKNOWN
        }
    }
}

enum class MessageType {
    TEXT, IMAGE, AUDIO, VIDEO, DOCUMENT, LOCATION, OTHER;

    companion object {
        /** Anything unrecognized is shown as a generic non-text message, never dropped. */
        fun from(raw: String?): MessageType = when (raw) {
            MessageTypeValues.TEXT -> TEXT
            MessageTypeValues.IMAGE -> IMAGE
            MessageTypeValues.AUDIO -> AUDIO
            MessageTypeValues.VIDEO -> VIDEO
            MessageTypeValues.DOCUMENT -> DOCUMENT
            MessageTypeValues.LOCATION -> LOCATION
            else -> OTHER
        }
    }
}

enum class DeliveryStatus {
    RECEIVED, PENDING, SENT, DELIVERED, READ, FAILED, UNKNOWN;

    companion object {
        fun from(raw: String?): DeliveryStatus = when (raw) {
            MessageStatusValues.RECEIVED -> RECEIVED
            MessageStatusValues.PENDING -> PENDING
            MessageStatusValues.SENT -> SENT
            MessageStatusValues.DELIVERED -> DELIVERED
            MessageStatusValues.READ -> READ
            MessageStatusValues.FAILED -> FAILED
            else -> UNKNOWN
        }
    }
}

/** A WhatsApp customer of the business. [name] is often null; fall back to the phone. */
data class Customer(
    val id: String,
    /** International digits without "+", e.g. 201000000001. */
    val phone: String,
    val name: String?
) {
    /** Phone formatted for display; blank when the backend sent none. */
    val displayPhone: String get() = if (phone.isBlank()) "" else "+$phone"

    /** Name when known, otherwise the phone. Blank only when both are missing (caller shows a label). */
    val title: String get() = name?.trim().takeUnless { it.isNullOrEmpty() } ?: displayPhone

    /** First letter of the name for the avatar, or null (caller draws a generic glyph). */
    val initial: String? get() = name?.trim()?.firstOrNull { it.isLetter() }?.uppercase()
}

data class ChatMessage(
    val id: String,
    val sender: SenderType,
    /** May be null/blank for media that the backend does not store yet. */
    val content: String?,
    val type: MessageType,
    val status: DeliveryStatus,
    val createdAt: Instant?
) {
    val hasText: Boolean get() = !content.isNullOrBlank()
}

data class Conversation(
    val id: String,
    val customer: Customer,
    val status: ConversationStatus,
    val aiEnabled: Boolean,
    val handoffReason: String?,
    val lastMessageAt: Instant?,
    /** Present on list items only; null on a single-conversation read. */
    val lastMessage: ChatMessage?
) {
    /** The AI handed the chat over: the owner is expected to answer. */
    val needsOwnerReply: Boolean get() = !aiEnabled && handoffReason != null
}

/** One page of a newest-first list. [nextOffset] is null on the last page. */
data class PagedList<T>(
    val items: List<T>,
    val totalCount: Int,
    val nextOffset: Int?
) {
    val hasMore: Boolean get() = nextOffset != null
}
