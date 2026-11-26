package com.yasminhosam.brandora.feature.catalogimport.presentation.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.GetPreviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val getPreview: GetPreviewUseCase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<PreviewUiState, PreviewEvent, PreviewEffect>(PreviewUiState()) {

    private val importId: String = savedStateHandle.get<String>(ApiParams.PATH_IMPORT_ID).orEmpty()

    init { loadFirstPage() }

    override fun onEvent(event: PreviewEvent) {
        when (event) {
            PreviewEvent.RetryClicked -> loadFirstPage()
            PreviewEvent.LoadMoreClicked -> loadMore()
            // One-shot: canImport stays true until an effect pops this screen, so a
            // double-tap must not push the Result step twice (each copy re-executes).
            PreviewEvent.ImportClicked -> {
                if (!uiState.value.isNavigating) {
                    setState { copy(isNavigating = true) }
                    sendEffect(PreviewEffect.NavigateToExecute(importId))
                }
            }
            PreviewEvent.BackToValidation -> {
                // One pop per tap: a second effect would keep popping past Validate.
                if (!uiState.value.isNavigating) {
                    setState { copy(isNavigating = true) }
                    sendEffect(PreviewEffect.NavigateBack)
                }
            }
        }
    }

    private fun loadFirstPage() {
        setState { copy(isLoading = true, errorRes = null, items = emptyList(), nextOffset = null) }
        viewModelScope.launch {
            when (val result = getPreview(importId, offset = 0)) {
                is ApiResult.Success -> setState {
                    copy(
                        isLoading = false,
                        summary = result.data.summary,
                        totalCount = result.data.page.totalCount,
                        items = result.data.items,
                        nextOffset = result.data.page.nextOffset
                    )
                }

                is ApiResult.Error -> setState {
                    copy(isLoading = false, errorRes = result.messageRes())
                }
            }
        }
    }

    private fun loadMore() {
        val offset = uiState.value.nextOffset ?: return
        if (uiState.value.isLoadingMore) return
        setState { copy(isLoadingMore = true, loadMoreErrorRes = null) }
        viewModelScope.launch {
            when (val result = getPreview(importId, offset)) {
                is ApiResult.Success -> setState {
                    copy(
                        isLoadingMore = false,
                        items = items + result.data.items,
                        nextOffset = result.data.page.nextOffset
                    )
                }

                is ApiResult.Error -> setState {
                    copy(isLoadingMore = false, loadMoreErrorRes = result.messageRes())
                }
            }
        }
    }
}
