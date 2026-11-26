package com.yasminhosam.brandora.feature.inbox.presentation.inbox

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.inbox.domain.usecase.GetConversationsUseCase
import com.yasminhosam.brandora.feature.inbox.presentation.util.appendDistinct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val getConversations: GetConversationsUseCase
) : BaseViewModel<InboxUiState, InboxEvent, InboxEffect>(InboxUiState()) {

    // Only one request runs at a time; a filter change cancels the previous one so a slow
    // response for the old filter can never overwrite the new list.
    private var loadJob: Job? = null

    init { loadFirstPage() }

    override fun onEvent(event: InboxEvent) {
        when (event) {
            is InboxEvent.FilterSelected -> {
                if (event.filter == uiState.value.filter) return
                setState {
                    copy(
                        filter = event.filter,
                        conversations = emptyList(),
                        nextOffset = null,
                        isLoading = true,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorRes = null,
                        loadMoreErrorRes = null
                    )
                }
                loadFirstPage()
            }
            InboxEvent.RetryClicked -> {
                setState { copy(isLoading = true, errorRes = null) }
                loadFirstPage()
            }
            InboxEvent.RefreshRequested -> {
                setState { copy(isRefreshing = true, isLoadingMore = false, loadMoreErrorRes = null) }
                loadFirstPage()
            }
            InboxEvent.LoadMoreClicked -> loadMore()
            is InboxEvent.ConversationClicked -> sendEffect(InboxEffect.OpenChat(event.conversationId))
        }
    }

    private fun loadFirstPage() {
        loadJob?.cancel()
        val status = uiState.value.filter.status
        loadJob = viewModelScope.launch {
            when (val result = getConversations(status = status, offset = 0)) {
                is ApiResult.Success -> setState {
                    copy(
                        conversations = result.data.items,
                        nextOffset = result.data.nextOffset,
                        isLoading = false,
                        isRefreshing = false,
                        errorRes = null
                    )
                }
                is ApiResult.Error -> setState {
                    // A failed pull-to-refresh keeps the old list visible instead of blanking it.
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorRes = if (conversations.isEmpty()) result.type.messageRes() else null,
                        loadMoreErrorRes = if (conversations.isEmpty()) null else result.type.messageRes()
                    )
                }
            }
        }
    }

    private fun loadMore() {
        val state = uiState.value
        val offset = state.nextOffset ?: return
        if (state.isLoadingMore || state.isLoading || state.isRefreshing) return
        setState { copy(isLoadingMore = true, loadMoreErrorRes = null) }
        loadJob = viewModelScope.launch {
            when (val result = getConversations(status = state.filter.status, offset = offset)) {
                is ApiResult.Success -> setState {
                    copy(
                        conversations = conversations.appendDistinct(result.data.items) { it.id },
                        nextOffset = result.data.nextOffset,
                        isLoadingMore = false
                    )
                }
                is ApiResult.Error -> setState {
                    copy(isLoadingMore = false, loadMoreErrorRes = result.type.messageRes())
                }
            }
        }
    }
}
