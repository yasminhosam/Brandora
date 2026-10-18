package com.yasminhosam.brandora.feature.catalogimport.presentation.validation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.GetValidationResultsUseCase
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.StartValidationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ValidationViewModel @Inject constructor(
    private val startValidation: StartValidationUseCase,
    private val getValidationResults: GetValidationResultsUseCase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<ValidationUiState, ValidationEvent, ValidationEffect>(ValidationUiState()) {

    private val importId: String = savedStateHandle.get<String>(ApiParams.PATH_IMPORT_ID).orEmpty()

    init { load() }

    override fun onEvent(event: ValidationEvent) {
        when (event) {
            ValidationEvent.RetryClicked -> load()
            ValidationEvent.LoadMoreClicked -> loadMore()
            ValidationEvent.ContinueClicked -> {
                if (uiState.value.canContinue) sendEffect(ValidationEffect.NavigateToPreview)
            }
            ValidationEvent.FixClicked -> sendEffect(ValidationEffect.NavigateToUpload)
        }
    }

    private fun load() {
        setState { copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = startValidation(importId)) {
                is ApiResult.Success -> setState {
                    copy(
                        summary = result.data.summary,
                        totalCount = result.data.page.totalCount,
                        nextOffset = result.data.page.nextOffset,
                        rows = emptyList()
                    )
                }.also { loadFirstPage() }

                is ApiResult.Error -> setState {
                    copy(isLoading = false, errorRes = result.type.messageRes())
                }
            }
        }
    }

    private fun loadFirstPage() {
        viewModelScope.launch {
            when (val result = getValidationResults(importId, offset = 0)) {
                is ApiResult.Success -> setState {
                    copy(
                        isLoading = false,
                        rows = result.data.rows,
                        nextOffset = result.data.page.nextOffset
                    )
                }

                is ApiResult.Error -> setState {
                    copy(isLoading = false, errorRes = result.type.messageRes())
                }
            }
        }
    }

    private fun loadMore() {
        val offset = uiState.value.nextOffset ?: return
        if (uiState.value.isLoadingMore) return
        setState { copy(isLoadingMore = true, loadMoreErrorRes = null) }
        viewModelScope.launch {
            when (val result = getValidationResults(importId, offset)) {
                is ApiResult.Success -> setState {
                    copy(
                        isLoadingMore = false,
                        rows = rows + result.data.rows,
                        nextOffset = result.data.page.nextOffset
                    )
                }

                is ApiResult.Error -> setState {
                    copy(isLoadingMore = false, loadMoreErrorRes = result.type.messageRes())
                }
            }
        }
    }
}
