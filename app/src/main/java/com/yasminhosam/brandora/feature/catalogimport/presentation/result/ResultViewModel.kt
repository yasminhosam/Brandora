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

    init { runImport() }

    override fun onEvent(event: ResultEvent) {
        when (event) {
            ResultEvent.RetryClicked -> runImport()
            ResultEvent.DoneClicked -> sendEffect(ResultEffect.NavigateToUpload)
        }
    }

    private fun runImport() {
        setState { copy(isLoading = true, errorRes = null, isJobGone = false) }
        viewModelScope.launch {
            when (val result = executeImport(importId)) {
                is ApiResult.Success -> when (val outcome = result.data) {
                    is ExecuteOutcome.Performed -> setState {
                        copy(
                            isLoading = false,
                            status = outcome.result.status,
                            counts = outcome.result.counts,
                            invalidRows = outcome.result.invalidRows,
                            failedRows = outcome.result.failedRows,
                            hasRowDetail = true
                        )
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
                        errorRes = result.type.messageRes(),
                        isJobGone = result.errorCode == ImportErrorCodes.IMPORT_JOB_NOT_FOUND
                    )
                }
            }
        }
    }
}
