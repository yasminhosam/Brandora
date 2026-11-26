package com.yasminhosam.brandora.feature.home.presentation

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.inbox.domain.usecase.GetOpenChatCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getOpenChatCount: GetOpenChatCountUseCase
) : BaseViewModel<HomeUiState, HomeEvent, HomeEffect>(HomeUiState()) {

    private var refreshJob: Job? = null

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            when (val result = getOpenChatCount()) {
                is ApiResult.Success -> setState { copy(openChatCount = result.data) }
                // Keep the last known count on failure; the badge is a hint, not an error state.
                is ApiResult.Error -> Unit
            }
        }
    }
}
