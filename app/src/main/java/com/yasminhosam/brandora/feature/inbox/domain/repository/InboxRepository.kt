package com.yasminhosam.brandora.feature.inbox.domain.repository

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer
import com.yasminhosam.brandora.feature.inbox.domain.model.PagedList

/** Read-only access to the owner's conversations. The business comes from the current business. */
interface InboxRepository {

    /** Customers, newest first. */
    suspend fun getCustomers(limit: Int, offset: Int): ApiResult<PagedList<Customer>>

    suspend fun getCustomer(customerId: String): ApiResult<Customer>

    /** Inbox page, newest activity first. A null [status] returns every conversation. */
    suspend fun getConversations(
        status: ConversationStatus?,
        limit: Int,
        offset: Int
    ): ApiResult<PagedList<Conversation>>

    suspend fun getConversation(conversationId: String): ApiResult<Conversation>

    /** Messages of one conversation, NEWEST FIRST (the backend's order; the UI reverses the layout). */
    suspend fun getMessages(
        conversationId: String,
        limit: Int,
        offset: Int
    ): ApiResult<PagedList<ChatMessage>>

    /**
     * Sends the owner's reply and returns the stored message (SENT, or FAILED when WhatsApp
     * refused it). The backend also switches the AI off for this conversation.
     */
    suspend fun sendMessage(conversationId: String, content: String): ApiResult<ChatMessage>

    /** Turns the AI on or off for this conversation and returns the updated conversation. */
    suspend fun setAiEnabled(conversationId: String, enabled: Boolean): ApiResult<Conversation>
}
