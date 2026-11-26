package com.yasminhosam.brandora.feature.inbox.presentation.chat

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatRules
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation

sealed interface ChatEvent : UiEvent {
    data object RetryClicked : ChatEvent
    data object RefreshRequested : ChatEvent
    data object LoadOlderClicked : ChatEvent
    data object HeaderClicked : ChatEvent
    data object SheetDismissed : ChatEvent
    data object OpenWhatsAppClicked : ChatEvent
    /** Composer text changed; the draft lives in state so rotation and sends keep it. */
    data class ReplyChanged(val text: String) : ChatEvent
    data object SendClicked : ChatEvent
    /** Re-sends a message that came back FAILED. The backend never resends the same row. */
    data class RetrySendClicked(val content: String) : ChatEvent
    data class AiToggled(val enabled: Boolean) : ChatEvent
}

data class ChatUiState(
    val conversation: Conversation? = null,
    /** NEWEST FIRST, exactly as the backend sends them (the list uses reverseLayout). */
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val nextOffset: Int? = null,
    val errorRes: Int? = null,
    val loadOlderErrorRes: Int? = null,
    val showCustomerSheet: Boolean = false,
    val draft: String = "",
    val isSending: Boolean = false,
    /** Why the last reply was not accepted (24-hour window, network...). Cleared on typing. */
    val sendErrorRes: Int? = null,
    val isTogglingAi: Boolean = false,
    val aiErrorRes: Int? = null
) : UiState {
    val hasOlder: Boolean get() = nextOffset != null
    val isEmpty: Boolean get() = !isLoading && errorRes == null && messages.isEmpty()
    val draftTooLong: Boolean get() = ChatRules.exceedsLimit(draft)
    val canSend: Boolean get() = !isSending && ChatRules.isSendable(draft)
}

sealed interface ChatEffect : UiEffect {
    /** Escape hatch to the customer's own WhatsApp chat (same thread, owner's personal app). */
    data class OpenWhatsApp(val phone: String) : ChatEffect
}
