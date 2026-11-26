package com.yasminhosam.brandora.feature.inbox.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import com.yasminhosam.brandora.core.network.ApiParams
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The owner's inbox: read the list/chat, send a reply and switch the AI on or off. Internal
 * because it exposes internal DTOs; only the RemoteDataSource creates it.
 */
internal interface InboxApiService {

    @GET(ApiEndpoints.CUSTOMER_LIST)
    suspend fun getCustomers(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String,
        @Query(ApiParams.QUERY_LIMIT) limit: Int,
        @Query(ApiParams.QUERY_OFFSET) offset: Int
    ): CustomersResponseDto

    @GET(ApiEndpoints.CUSTOMER)
    suspend fun getCustomer(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String,
        @Path(ApiParams.CUSTOMER_ID) customerId: String
    ): CustomerDto

    @GET(ApiEndpoints.CONVERSATION_LIST)
    suspend fun getConversations(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String,
        @Query(ApiParams.QUERY_STATUS) status: String?,
        @Query(ApiParams.QUERY_LIMIT) limit: Int,
        @Query(ApiParams.QUERY_OFFSET) offset: Int
    ): ConversationsResponseDto

    @GET(ApiEndpoints.CONVERSATION)
    suspend fun getConversation(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String,
        @Path(ApiParams.CONVERSATION_ID) conversationId: String
    ): ConversationDto

    @GET(ApiEndpoints.CONVERSATION_MESSAGES)
    suspend fun getMessages(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String,
        @Path(ApiParams.CONVERSATION_ID) conversationId: String,
        @Query(ApiParams.QUERY_LIMIT) limit: Int,
        @Query(ApiParams.QUERY_OFFSET) offset: Int
    ): MessagesResponseDto

    /**
     * Sends the owner's text on WhatsApp (201). The stored message comes back as SENT, or FAILED
     * when Meta refused it; 409 WHATSAPP_WINDOW_CLOSED means the 24-hour reply window is over.
     */
    @POST(ApiEndpoints.CONVERSATION_MESSAGES)
    suspend fun sendMessage(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String,
        @Path(ApiParams.CONVERSATION_ID) conversationId: String,
        @Body body: SendMessageRequestDto
    ): MessageDto

    /** AI switch: `false` hands the chat to the owner, `true` lets the AI answer again. */
    @PATCH(ApiEndpoints.CONVERSATION)
    suspend fun updateConversation(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String,
        @Path(ApiParams.CONVERSATION_ID) conversationId: String,
        @Body body: UpdateConversationRequestDto
    ): ConversationDto
}
