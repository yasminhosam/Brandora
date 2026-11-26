package com.yasminhosam.brandora.feature.inbox

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer
import com.yasminhosam.brandora.feature.inbox.domain.model.DeliveryStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.MessageType
import com.yasminhosam.brandora.feature.inbox.domain.model.PagedList
import com.yasminhosam.brandora.feature.inbox.domain.model.SenderType
import com.yasminhosam.brandora.feature.inbox.domain.repository.InboxRepository
import java.time.Instant

fun customer(id: String = "c1", name: String? = "Mona") = Customer(id, "201000000001", name)

fun message(id: String, at: String = "2026-11-16T10:00:00Z", sender: SenderType = SenderType.CUSTOMER) =
    ChatMessage(id, sender, "text $id", MessageType.TEXT, DeliveryStatus.RECEIVED, Instant.parse(at))

fun conversation(id: String, status: ConversationStatus = ConversationStatus.OPEN) = Conversation(
    id = id,
    customer = customer(),
    status = status,
    aiEnabled = true,
    handoffReason = null,
    lastMessageAt = Instant.parse("2026-11-16T10:00:00Z"),
    lastMessage = message("m-$id")
)

/** Scriptable fake: set the result per call; calls are recorded. */
class FakeInboxRepository : InboxRepository {
    var conversationsResult: (ConversationStatus?, Int) -> ApiResult<PagedList<Conversation>> =
        { _, _ -> ApiResult.Success(PagedList(emptyList(), 0, null)) }
    var conversationResult: ApiResult<Conversation> = ApiResult.Success(conversation("c1"))
    var messagesResult: (Int) -> ApiResult<PagedList<ChatMessage>> =
        { ApiResult.Success(PagedList(emptyList(), 0, null)) }
    var sendResult: (String) -> ApiResult<ChatMessage> =
        { ApiResult.Success(message("sent", sender = SenderType.OWNER)) }
    var aiResult: (Boolean) -> ApiResult<Conversation> =
        { enabled -> ApiResult.Success(conversation("c1").copy(aiEnabled = enabled)) }

    val conversationCalls = mutableListOf<Pair<ConversationStatus?, Int>>()
    val messageOffsets = mutableListOf<Int>()
    val sentMessages = mutableListOf<String>()
    val aiCalls = mutableListOf<Boolean>()

    override suspend fun getCustomers(limit: Int, offset: Int): ApiResult<PagedList<Customer>> =
        ApiResult.Success(PagedList(emptyList(), 0, null))

    override suspend fun getCustomer(customerId: String): ApiResult<Customer> =
        ApiResult.Success(customer(customerId))

    override suspend fun getConversations(
        status: ConversationStatus?, limit: Int, offset: Int
    ): ApiResult<PagedList<Conversation>> {
        conversationCalls += status to offset
        return conversationsResult(status, offset)
    }

    override suspend fun getConversation(conversationId: String) = conversationResult

    override suspend fun getMessages(conversationId: String, limit: Int, offset: Int): ApiResult<PagedList<ChatMessage>> {
        messageOffsets += offset
        return messagesResult(offset)
    }

    override suspend fun sendMessage(conversationId: String, content: String): ApiResult<ChatMessage> {
        sentMessages += content
        return sendResult(content)
    }

    override suspend fun setAiEnabled(conversationId: String, enabled: Boolean): ApiResult<Conversation> {
        aiCalls += enabled
        return aiResult(enabled)
    }
}
