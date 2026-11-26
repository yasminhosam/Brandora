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

    /** One startValidation POST at a time: a double-tapped Retry must not race (a repeat can 409). */
    private var startValidationInFlight = false

    init { load() }

    override fun onEvent(event: ValidationEvent) {
        when (event) {
            ValidationEvent.ScreenEntered -> setState { copy(isNavigating = false) }
            ValidationEvent.RetryClicked -> load()
            ValidationEvent.LoadMoreClicked -> loadMore()
            ValidationEvent.ContinueClicked -> {
                // canContinue includes the latch, so setting it here makes a double-tap send
                // exactly one effect; ScreenEntered re-arms it on return from Preview.
                if (uiState.value.canContinue) {
                    setState { copy(isNavigating = true) }
                    sendEffect(ValidationEffect.NavigateToPreview)
                }
            }
            ValidationEvent.FixClicked -> {
                // One pop per tap: a second effect would keep popping past Upload.
                if (!uiState.value.isNavigating) {
                    setState { copy(isNavigating = true) }
                    sendEffect(ValidationEffect.NavigateToUpload)
                }
            }
        }
    }

    private fun load() {
        // Skip re-triggering validation when it is already READY (e.g. ViewModel restored after
        // process death or re-navigation back to this screen). A repeat POST can return HTTP 409.
        if (uiState.value.isValidationReady) {
            loadFirstPage()
            return
        }
        if (startValidationInFlight) return
        startValidationInFlight = true
        setState { copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            try {
                when (val result = startValidation(importId)) {
                    is ApiResult.Success -> setState {
                        copy(
                            status = result.data.status,
                            summary = result.data.summary,
                            totalCount = result.data.page.totalCount,
                            nextOffset = result.data.page.nextOffset,
                            rows = emptyList()
                        )
                    }.also { loadFirstPage() }

                    is ApiResult.Error -> setState {
                        copy(isLoading = false, errorRes = result.messageRes())
                    }
                }
            } finally {
                startValidationInFlight = false
            }
        }
    }

    private fun loadFirstPage() {
        // Clear the previous failure before retrying; otherwise a successful reload leaves
        // errorRes set and the error screen stays on top of the fresh rows.
        setState { copy(isLoading = true, errorRes = null, loadMoreErrorRes = null) }
        viewModelScope.launch {
            when (val result = getValidationResults(importId, offset = 0)) {
                is ApiResult.Success -> setState {
                    copy(
                        isLoading = false,
                        errorRes = null,
                        rows = result.data.rows,
                        nextOffset = result.data.page.nextOffset,
                        loadMoreErrorRes = null
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
            when (val result = getValidationResults(importId, offset)) {
                is ApiResult.Success -> setState {
                    copy(
                        isLoadingMore = false,
                        rows = rows + result.data.rows,
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
