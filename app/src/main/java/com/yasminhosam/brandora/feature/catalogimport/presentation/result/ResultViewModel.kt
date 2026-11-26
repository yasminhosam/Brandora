package com.yasminhosam.brandora.feature.catalogimport.presentation.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.ImportErrorCodes
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteOutcome
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.ExecuteImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val executeImport: ExecuteImportUseCase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<ResultUiState, ResultEvent, ResultEffect>(ResultUiState()) {

    private val importId: String = savedStateHandle.get<String>(ApiParams.PATH_IMPORT_ID).orEmpty()

    /** Done pops the whole flow; it must fire exactly once, even on a double-tap. */
    private var doneSent = false

    init { runImport() }

    override fun onEvent(event: ResultEvent) {
        when (event) {
            // isLoading already means "execute in flight": runImport() flips it synchronously,
            // so a double-tapped Retry/CheckStatus can never stack two concurrent executes
            // (the loser response would overwrite the winner's state).
            ResultEvent.RetryClicked, ResultEvent.CheckStatusClicked ->
                if (!uiState.value.isLoading) runImport()

            ResultEvent.DoneClicked -> {
                if (doneSent) return
                doneSent = true
                if (uiState.value.isCompleted || uiState.value.isPartial) {
                    sendEffect(ResultEffect.NavigateToHome)
                } else {
                    sendEffect(ResultEffect.NavigateToUpload)
                }
            }
        }
    }

    private fun runImport() {
        setState { copy(isLoading = true, errorRes = null, isJobGone = false) }
        viewModelScope.launch {
            when (val result = executeImport(importId)) {
                is ApiResult.Success -> when (val outcome = result.data) {
                    is ExecuteOutcome.Performed -> {
                        val status = outcome.result.status
                        setState {
                            copy(
                                isLoading = false,
                                status = status,
                                counts = outcome.result.counts,
                                invalidRows = outcome.result.invalidRows,
                                failedRows = outcome.result.failedRows,
                                hasRowDetail = true
                            )
                        }
                        // Stay on the result screen; navigation home happens only when the
                        // user taps Done (see ResultEvent.DoneClicked).
                    }

                    is ExecuteOutcome.Recovered -> setState {
                        copy(
                            isLoading = false,
                            status = outcome.status.status,
                            counts = outcome.status.counts,
                            hasRowDetail = false
                        )
                    }
                }

                is ApiResult.Error -> setState {
                    copy(
                        isLoading = false,
                        errorRes = result.messageRes(),
                        isJobGone = result.errorCode == ImportErrorCodes.IMPORT_JOB_NOT_FOUND
                    )
                }
            }
        }
    }
}
