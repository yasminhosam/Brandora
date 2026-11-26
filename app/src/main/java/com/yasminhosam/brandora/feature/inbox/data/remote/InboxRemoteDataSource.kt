package com.yasminhosam.brandora.feature.inbox.data.remote

import com.yasminhosam.brandora.core.network.ApiErrorHandler
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.map
import com.yasminhosam.brandora.feature.inbox.data.mapper.toDomain
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer
import com.yasminhosam.brandora.feature.inbox.domain.model.PagedList
import retrofit2.Retrofit
import javax.inject.Inject

class InboxRemoteDataSource @Inject constructor(
    retrofit: Retrofit,
    private val errorHandler: ApiErrorHandler
) {
    private val api: InboxApiService = retrofit.create(InboxApiService::class.java)

    suspend fun getCustomers(businessId: String, limit: Int, offset: Int): ApiResult<PagedList<Customer>> =
        errorHandler.safeApiCall { api.getCustomers(businessId, limit, offset) }.map { it.toDomain() }

    suspend fun getCustomer(businessId: String, customerId: String): ApiResult<Customer> =
        errorHandler.safeApiCall { api.getCustomer(businessId, customerId) }.map { it.toDomain() }

    suspend fun getConversations(
        businessId: String,
        status: ConversationStatus?,
        limit: Int,
        offset: Int
    ): ApiResult<PagedList<Conversation>> =
        errorHandler.safeApiCall {
            api.getConversations(businessId, status.toQuery(), limit, offset)
        }.map { it.toDomain() }

    suspend fun getConversation(businessId: String, conversationId: String): ApiResult<Conversation> =
        errorHandler.safeApiCall { api.getConversation(businessId, conversationId) }.map { it.toDomain() }

    suspend fun getMessages(
        businessId: String,
        conversationId: String,
        limit: Int,
        offset: Int
    ): ApiResult<PagedList<ChatMessage>> =
        errorHandler.safeApiCall {
            api.getMessages(businessId, conversationId, limit, offset)
        }.map { it.toDomain() }

    suspend fun sendMessage(
        businessId: String,
        conversationId: String,
        content: String
    ): ApiResult<ChatMessage> =
        errorHandler.safeApiCall {
            api.sendMessage(businessId, conversationId, SendMessageRequestDto(content))
        }.map { it.toDomain() }

    suspend fun setAiEnabled(
        businessId: String,
        conversationId: String,
        enabled: Boolean
    ): ApiResult<Conversation> =
        errorHandler.safeApiCall {
            api.updateConversation(businessId, conversationId, UpdateConversationRequestDto(enabled))
        }.map { it.toDomain() }

    /** null / UNKNOWN omit the filter (backend returns every status). */
    private fun ConversationStatus?.toQuery(): String? = when (this) {
        ConversationStatus.OPEN -> "OPEN"
        ConversationStatus.CLOSED -> "CLOSED"
        else -> null
    }
}
