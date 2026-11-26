package com.yasminhosam.brandora.feature.inbox.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.HandoffReasonValues
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatRules
import com.yasminhosam.brandora.feature.inbox.domain.usecase.GetConversationUseCase
import com.yasminhosam.brandora.feature.inbox.domain.usecase.GetMessagesUseCase
import com.yasminhosam.brandora.feature.inbox.domain.usecase.SendMessageUseCase
import com.yasminhosam.brandora.feature.inbox.domain.usecase.SetAiEnabledUseCase
import com.yasminhosam.brandora.feature.inbox.presentation.util.appendDistinct
import com.yasminhosam.brandora.feature.inbox.presentation.util.prependDistinct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getConversation: GetConversationUseCase,
    private val getMessages: GetMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
    private val setAiEnabled: SetAiEnabledUseCase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<ChatUiState, ChatEvent, ChatEffect>(ChatUiState()) {

    private val conversationId: String =
        savedStateHandle.get<String>(ApiParams.CONVERSATION_ID).orEmpty()

    init { loadFirstPage() }

    override fun onEvent(event: ChatEvent) {
        when (event) {
            ChatEvent.RetryClicked -> {
                setState { copy(isLoading = true, errorRes = null) }
                loadFirstPage()
            }
            ChatEvent.RefreshRequested -> {
                setState { copy(isRefreshing = true) }
                loadFirstPage()
            }
            ChatEvent.LoadOlderClicked -> loadOlder()
            ChatEvent.HeaderClicked -> if (uiState.value.conversation != null) {
                setState { copy(showCustomerSheet = true) }
            }
            ChatEvent.SheetDismissed -> setState { copy(showCustomerSheet = false) }
            ChatEvent.OpenWhatsAppClicked -> uiState.value.conversation?.customer?.phone
                ?.takeIf { it.isNotBlank() }
                ?.let { sendEffect(ChatEffect.OpenWhatsApp(it)) }
            is ChatEvent.ReplyChanged ->
                setState { copy(draft = event.text, sendErrorRes = null) }
            ChatEvent.SendClicked -> send(uiState.value.draft, clearDraft = true)
            is ChatEvent.RetrySendClicked -> send(event.content, clearDraft = false)
            is ChatEvent.AiToggled -> toggleAi(event.enabled)
        }
    }

    /** Header and first message page load in parallel; the screen needs both to be useful. */
    private fun loadFirstPage() {
        viewModelScope.launch {
            val header = async { getConversation(conversationId) }
            val page = async { getMessages(conversationId, offset = 0) }
            val headerResult = header.await()
            val pageResult = page.await()

            val error = (headerResult as? ApiResult.Error) ?: (pageResult as? ApiResult.Error)
            if (error != null) {
                setState {
                    // A failed refresh keeps what is already on screen.
                    if (conversation != null) copy(isLoading = false, isRefreshing = false)
                    else copy(isLoading = false, isRefreshing = false, errorRes = error.type.messageRes())
                }
                return@launch
            }
            val conversation = (headerResult as ApiResult.Success).data
            val messages = (pageResult as ApiResult.Success).data
            setState {
                copy(
                    conversation = conversation,
                    messages = messages.items,
                    nextOffset = messages.nextOffset,
                    isLoading = false,
                    isRefreshing = false,
                    errorRes = null,
                    loadOlderErrorRes = null
                )
            }
        }
    }

    private fun loadOlder() {
        val state = uiState.value
        val offset = state.nextOffset ?: return
        if (state.isLoadingOlder || state.isLoading || state.isRefreshing) return
        setState { copy(isLoadingOlder = true, loadOlderErrorRes = null) }
        viewModelScope.launch {
            when (val result = getMessages(conversationId, offset = offset)) {
                is ApiResult.Success -> setState {
                    copy(
                        messages = messages.appendDistinct(result.data.items) { it.id },
                        nextOffset = result.data.nextOffset,
                        isLoadingOlder = false
                    )
                }
                is ApiResult.Error -> setState {
                    copy(isLoadingOlder = false, loadOlderErrorRes = result.type.messageRes())
                }
            }
        }
    }

    /**
     * POSTs the reply. The answer is the stored message (SENT, or FAILED when WhatsApp refused
     * it), so it goes to the top of the newest-first list. The draft is only cleared when the
     * request itself succeeded; a retry of a FAILED message is a new message, not the same row.
     */
    private fun send(text: String, clearDraft: Boolean) {
        val content = text.trim()
        val state = uiState.value
        if (state.isSending || !ChatRules.isSendable(content)) return
        setState { copy(isSending = true, sendErrorRes = null) }
        viewModelScope.launch {
            when (val result = sendMessage(conversationId, content)) {
                is ApiResult.Success -> setState {
                    copy(
                        messages = messages.prependDistinct(listOf(result.data)) { it.id },
                        // Only clear what was actually sent: the owner may have typed more
                        // while the request was in flight.
                        draft = if (clearDraft && draft == text) "" else draft,
                        isSending = false,
                        sendErrorRes = null,
                        // The backend makes the AI step back when the owner replies, so the
                        // switch must not keep claiming the AI is answering.
                        conversation = conversation?.copy(
                            aiEnabled = false,
                            handoffReason = HandoffReasonValues.OWNER_TOOK_OVER
                        )
                    )
                }
                is ApiResult.Error -> setState {
                    copy(isSending = false, sendErrorRes = result.messageRes())
                }
            }
        }
    }

    private fun toggleAi(enabled: Boolean) {
        val state = uiState.value
        val conversation = state.conversation ?: return
        if (state.isTogglingAi || conversation.aiEnabled == enabled) return
        setState { copy(isTogglingAi = true, aiErrorRes = null) }
        viewModelScope.launch {
            when (val result = setAiEnabled(conversationId, enabled)) {
                is ApiResult.Success -> setState {
                    // The PATCH answer has no lastMessage; keep the one the inbox list gave us.
                    copy(
                        conversation = result.data.copy(lastMessage = this.conversation?.lastMessage),
                        isTogglingAi = false
                    )
                }
                is ApiResult.Error -> setState {
                    copy(isTogglingAi = false, aiErrorRes = result.messageRes())
                }
            }
        }
    }
}
