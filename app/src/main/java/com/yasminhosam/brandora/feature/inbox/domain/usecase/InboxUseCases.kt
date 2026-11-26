package com.yasminhosam.brandora.feature.inbox.domain.usecase

import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.map
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer
import com.yasminhosam.brandora.feature.inbox.domain.model.PagedList
import com.yasminhosam.brandora.feature.inbox.domain.repository.InboxRepository
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(
    private val repository: InboxRepository
) {
    suspend operator fun invoke(
        status: ConversationStatus?,
        offset: Int = ApiConfig.DEFAULT_OFFSET,
        limit: Int = ApiConfig.DEFAULT_LIMIT
    ): ApiResult<PagedList<Conversation>> = repository.getConversations(status, limit, offset)
}

class GetConversationUseCase @Inject constructor(
    private val repository: InboxRepository
) {
    suspend operator fun invoke(conversationId: String): ApiResult<Conversation> =
        repository.getConversation(conversationId)
}

class GetMessagesUseCase @Inject constructor(
    private val repository: InboxRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        offset: Int = ApiConfig.DEFAULT_OFFSET,
        limit: Int = ApiConfig.DEFAULT_LIMIT
    ): ApiResult<PagedList<ChatMessage>> = repository.getMessages(conversationId, limit, offset)
}

/** Owner reply. The caller trims and length-checks; the backend validates again. */
class SendMessageUseCase @Inject constructor(
    private val repository: InboxRepository
) {
    suspend operator fun invoke(conversationId: String, content: String): ApiResult<ChatMessage> =
        repository.sendMessage(conversationId, content)
}

class SetAiEnabledUseCase @Inject constructor(
    private val repository: InboxRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        enabled: Boolean
    ): ApiResult<Conversation> = repository.setAiEnabled(conversationId, enabled)
}

/**
 * Number of OPEN chats for the Home badge. The backend has no unread count, so this reuses
 * the list endpoint with a one-item page and reads only its totalCount.
 */
class GetOpenChatCountUseCase @Inject constructor(
    private val repository: InboxRepository
) {
    suspend operator fun invoke(): ApiResult<Int> =
        repository.getConversations(ConversationStatus.OPEN, limit = 1, offset = 0)
            .map { it.totalCount }
}

class GetCustomersUseCase @Inject constructor(
    private val repository: InboxRepository
) {
    suspend operator fun invoke(
        offset: Int = ApiConfig.DEFAULT_OFFSET,
        limit: Int = ApiConfig.DEFAULT_LIMIT
    ): ApiResult<PagedList<Customer>> = repository.getCustomers(limit, offset)
}

class GetCustomerUseCase @Inject constructor(
    private val repository: InboxRepository
) {
    suspend operator fun invoke(customerId: String): ApiResult<Customer> =
        repository.getCustomer(customerId)
}
