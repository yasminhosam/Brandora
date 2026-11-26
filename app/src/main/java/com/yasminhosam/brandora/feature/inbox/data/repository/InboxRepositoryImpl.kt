package com.yasminhosam.brandora.feature.inbox.data.repository

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import com.yasminhosam.brandora.feature.inbox.data.remote.InboxRemoteDataSource
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer
import com.yasminhosam.brandora.feature.inbox.domain.model.PagedList
import com.yasminhosam.brandora.feature.inbox.domain.repository.InboxRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Refuses to call the backend without a current business rather than sending a placeholder id. */
class InboxRepositoryImpl @Inject constructor(
    private val remote: InboxRemoteDataSource,
    private val currentBusinessStore: CurrentBusinessStore
) : InboxRepository {

    override suspend fun getCustomers(limit: Int, offset: Int): ApiResult<PagedList<Customer>> {
        val businessId = currentBusinessStore.businessId.first() ?: return noBusiness()
        return remote.getCustomers(businessId, limit, offset)
    }

    override suspend fun getCustomer(customerId: String): ApiResult<Customer> {
        val businessId = currentBusinessStore.businessId.first() ?: return noBusiness()
        return remote.getCustomer(businessId, customerId)
    }

    override suspend fun getConversations(
        status: ConversationStatus?,
        limit: Int,
        offset: Int
    ): ApiResult<PagedList<Conversation>> {
        val businessId = currentBusinessStore.businessId.first() ?: return noBusiness()
        return remote.getConversations(businessId, status, limit, offset)
    }

    override suspend fun getConversation(conversationId: String): ApiResult<Conversation> {
        val businessId = currentBusinessStore.businessId.first() ?: return noBusiness()
        return remote.getConversation(businessId, conversationId)
    }

    override suspend fun getMessages(
        conversationId: String,
        limit: Int,
        offset: Int
    ): ApiResult<PagedList<ChatMessage>> {
        val businessId = currentBusinessStore.businessId.first() ?: return noBusiness()
        return remote.getMessages(businessId, conversationId, limit, offset)
    }

    override suspend fun sendMessage(conversationId: String, content: String): ApiResult<ChatMessage> {
        val businessId = currentBusinessStore.businessId.first() ?: return noBusiness()
        return remote.sendMessage(businessId, conversationId, content)
    }

    override suspend fun setAiEnabled(
        conversationId: String,
        enabled: Boolean
    ): ApiResult<Conversation> {
        val businessId = currentBusinessStore.businessId.first() ?: return noBusiness()
        return remote.setAiEnabled(businessId, conversationId, enabled)
    }

    private fun noBusiness() = ApiResult.Error(ApiErrorType.UNKNOWN)
}
