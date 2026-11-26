package com.yasminhosam.brandora.feature.inbox.data.mapper

import com.yasminhosam.brandora.core.network.dto.PaginationDto
import com.yasminhosam.brandora.feature.inbox.data.remote.ConversationCustomerDto
import com.yasminhosam.brandora.feature.inbox.data.remote.ConversationDto
import com.yasminhosam.brandora.feature.inbox.data.remote.ConversationsResponseDto
import com.yasminhosam.brandora.feature.inbox.data.remote.CustomerDto
import com.yasminhosam.brandora.feature.inbox.data.remote.CustomersResponseDto
import com.yasminhosam.brandora.feature.inbox.data.remote.MessageDto
import com.yasminhosam.brandora.feature.inbox.data.remote.MessagesResponseDto
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer
import com.yasminhosam.brandora.feature.inbox.domain.model.DeliveryStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.MessageType
import com.yasminhosam.brandora.feature.inbox.domain.model.PagedList
import com.yasminhosam.brandora.feature.inbox.domain.model.SenderType
import java.time.Instant
import java.time.format.DateTimeParseException

/** ISO-8601 UTC ("2026-11-16T10:00:00.000Z"). A bad or missing value becomes null, never a crash. */
internal fun String?.toInstantOrNull(): Instant? =
    if (isNullOrBlank()) null else try {
        Instant.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }

internal fun ConversationCustomerDto.toDomain() = Customer(id = id, phone = phone, name = name)

internal fun CustomerDto.toDomain() = Customer(id = id, phone = phone, name = name)

internal fun MessageDto.toDomain() = ChatMessage(
    id = id,
    sender = SenderType.from(senderType),
    content = content,
    type = MessageType.from(messageType),
    status = DeliveryStatus.from(status),
    createdAt = createdAt.toInstantOrNull()
)

internal fun ConversationDto.toDomain() = Conversation(
    id = id,
    customer = (customer ?: ConversationCustomerDto(id = customerId.orEmpty())).toDomain(),
    status = ConversationStatus.from(status),
    aiEnabled = aiEnabled,
    handoffReason = handoffReason?.takeIf { it.isNotBlank() },
    lastMessageAt = lastMessageAt.toInstantOrNull(),
    lastMessage = lastMessage?.toDomain()
)

private fun PaginationDto.resolvedNextOffset(): Int? =
    if (hasMore) nextOffset else null

internal fun ConversationsResponseDto.toDomain() = PagedList(
    items = items.map { it.toDomain() },
    totalCount = pagination.totalCount,
    nextOffset = pagination.resolvedNextOffset()
)

internal fun MessagesResponseDto.toDomain() = PagedList(
    items = items.map { it.toDomain() },
    totalCount = pagination.totalCount,
    nextOffset = pagination.resolvedNextOffset()
)

internal fun CustomersResponseDto.toDomain() = PagedList(
    items = items.map { it.toDomain() },
    totalCount = pagination.totalCount,
    nextOffset = pagination.resolvedNextOffset()
)
