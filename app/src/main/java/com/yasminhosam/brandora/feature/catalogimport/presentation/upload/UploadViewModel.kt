package com.yasminhosam.brandora.feature.catalogimport.presentation.upload

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.file.ExcelFileFactory
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.AnalyzeExcelFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val analyzeExcelFile: AnalyzeExcelFileUseCase,
    private val excelFileFactory: ExcelFileFactory
) : BaseViewModel<UploadUiState, UploadEvent, UploadEffect>(UploadUiState()) {

    override fun onEvent(event: UploadEvent) {
        when (event) {
            is UploadEvent.FilePicked -> onFilePicked(event.uri)
            UploadEvent.ContinueClicked -> onContinue()
            UploadEvent.ManualEntryClicked -> sendEffect(UploadEffect.NavigateToManualEntry)
        }
    }

    private fun onFilePicked(uri: String) {
        val file = excelFileFactory.create(uri)
        if (file == null) {
            setState { copy(file = null, fileError = ApiErrorType.UNKNOWN, uploadError = null) }
            return
        }
        val invalid = analyzeExcelFile.validate(file)
        setState {
            copy(
                file = if (invalid == null) file else null,
                fileError = invalid,
                uploadError = null
            )
        }
    }

    private fun onContinue() {
        val state = uiState.value
        val file = state.file
        if (file == null || !state.canContinue) return
        setState { copy(isAnalyzing = true, uploadError = null) }
        viewModelScope.launch {
            when (val result = analyzeExcelFile(file)) {
                is ApiResult.Success -> {
                    setState { copy(isAnalyzing = false) }
                    sendEffect(UploadEffect.NavigateToMapping(result.data.importJobId))
                }

                is ApiResult.Error ->
                    setState { copy(isAnalyzing = false, uploadError = result.type) }
            }
        }
    }
}
