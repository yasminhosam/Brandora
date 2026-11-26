package com.yasminhosam.brandora.feature.inbox.data.remote

import com.yasminhosam.brandora.core.network.dto.PaginationDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ConversationCustomerDto(
    val id: String,
    val phone: String = "",
    val name: String? = null,
)

internal typealias CustomerSummaryDto = ConversationCustomerDto

@Serializable
internal data class MessageDto(
    val id: String,
    val conversationId: String? = null,
    val senderType: String? = null,
    val externalMessageId: String? = null,
    val content: String? = null,
    val messageType: String? = null,
    val status: String? = null,
    val aiMeta: JsonObject? = null,
    val createdAt: String? = null,
)

@Serializable
internal data class ConversationDto(
    val id: String,
    val businessId: String? = null,
    val customerId: String? = null,
    val channel: String? = null,
    val status: String? = null,
    val aiEnabled: Boolean = true,
    val handoffReason: String? = null,
    val lastMessageAt: String? = null,
    val lastCustomerMessageAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val customer: ConversationCustomerDto? = null,
    val lastMessage: MessageDto? = null,
)

@Serializable
internal data class ConversationsResponseDto(
    val pagination: PaginationDto = PaginationDto(),
    val items: List<ConversationDto> = emptyList(),
)

@Serializable
internal data class CustomerDto(
    val id: String,
    val businessId: String? = null,
    val phone: String = "",
    val name: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
internal data class CustomersResponseDto(
    val pagination: PaginationDto = PaginationDto(),
    val items: List<CustomerDto> = emptyList(),
)

@Serializable
internal data class MessagesResponseDto(
    val pagination: PaginationDto = PaginationDto(),
    val items: List<MessageDto> = emptyList(),
)

/** Body of POST .../conversations/{conversationId}/messages. 1-4096 code points, trimmed. */
@Serializable
internal data class SendMessageRequestDto(
    val content: String,
)

/** Body of PATCH .../conversations/{conversationId}: the only mutable setting is the AI switch. */
@Serializable
internal data class UpdateConversationRequestDto(
    val aiEnabled: Boolean,
)
