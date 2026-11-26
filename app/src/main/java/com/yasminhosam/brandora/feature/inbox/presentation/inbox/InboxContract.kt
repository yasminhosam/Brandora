package com.yasminhosam.brandora.feature.inbox.presentation.inbox

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus

enum class InboxFilter(val status: ConversationStatus?) {
    OPEN(ConversationStatus.OPEN),
    CLOSED(ConversationStatus.CLOSED),
    ALL(null)
}

sealed interface InboxEvent : UiEvent {
    data class FilterSelected(val filter: InboxFilter) : InboxEvent
    data object RetryClicked : InboxEvent
    data object RefreshRequested : InboxEvent
    /** The screen resumed: refresh on return from Chat, but not on the first composition. */
    data object ScreenResumed : InboxEvent
    data object LoadMoreClicked : InboxEvent
    data class ConversationClicked(val conversationId: String) : InboxEvent
}

data class InboxUiState(
    val filter: InboxFilter = InboxFilter.OPEN,
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextOffset: Int? = null,
    val errorRes: Int? = null,
    /** A failed refresh with an existing list; kept separate from pagination failures. */
    val refreshErrorRes: Int? = null,
    val loadMoreErrorRes: Int? = null
) : UiState {
    val hasMore: Boolean get() = nextOffset != null
    val isEmpty: Boolean get() = !isLoading && errorRes == null && conversations.isEmpty()
}

sealed interface InboxEffect : UiEffect {
    data class OpenChat(val conversationId: String) : InboxEffect
}
