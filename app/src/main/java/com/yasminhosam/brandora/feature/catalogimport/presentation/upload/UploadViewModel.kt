package com.yasminhosam.brandora.feature.catalogimport.presentation.upload

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.file.ExcelFileFactory
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.AnalyzeExcelFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class UploadViewModel
@Inject
constructor(
    private val analyzeExcelFile: AnalyzeExcelFileUseCase,
    private val excelFileFactory: ExcelFileFactory,
) : BaseViewModel<UploadUiState, UploadEvent, UploadEffect>(UploadUiState()) {

    /** The in-flight analysis; a new file pick cancels it instead of being silently dropped. */
    private var analysisJob: Job? = null

    override fun onEvent(event: UploadEvent) {
        if (uiState.value.isNavigating && event != UploadEvent.ScreenEntered) return
        if (uiState.value.isAnalyzing) {
            // Analysis only yields to a genuinely new file (a redelivered picker result must
            // not be swallowed and leave the user staring at the previous file); everything
            // else waits for it to finish.
            if (event !is UploadEvent.FilePicked) return
            analysisJob?.cancel()
            setState { copy(isAnalyzing = false) }
        }
        when (event) {
            UploadEvent.ScreenEntered -> {
                if (uiState.value.isNavigating)
                    setState { copy(isNavigating = false, analysis = null, chosenSheet = null) }
            }
            is UploadEvent.SheetChosen -> {
                if (uiState.value.analysis?.sheetNames?.contains(event.name) == true)
                    setState { copy(chosenSheet = event.name) }
            }
            is UploadEvent.FilePicked -> onFilePicked(event.uri)
            UploadEvent.ContinueClicked -> onContinue()
            UploadEvent.SkipClicked -> sendEffect(UploadEffect.NavigateToNextStep)
        }
    }

    private fun onFilePicked(uri: String) {
        val file = excelFileFactory.create(uri)
        if (file == null) {
            setState {
                copy(
                    file = null,
                    analysis = null,
                    chosenSheet = null,
                    fileError = ApiErrorType.UNKNOWN,
                    uploadError = null,
                )
            }
            return
        }
        val invalid = analyzeExcelFile.validate(file)
        setState {
            copy(
                analysis = null,
                chosenSheet = null,
                file = if (invalid == null) file else null,
                fileError = invalid,
                uploadError = null,
            )
        }
    }

    private fun onContinue() {
        val state = uiState.value
        val file = state.file
        if (file == null || !state.canContinue) return
        val sheet = state.chosenSheet
        val current = state.analysis
        // Skip re-analysis only on an EXPLICIT sheet match: "never chose a sheet" and
        // "backend named no sheet" are both null, and null == null must not wave a
        // multi-sheet workbook through without a choice.
        if (current != null && sheet != null && sheet == current.selectedSheetName) {
            setState { copy(isNavigating = true) }
            sendEffect(UploadEffect.NavigateToMapping(current.importJobId))
            return
        }
        setState { copy(isAnalyzing = true, uploadError = null) }
        analysisJob = viewModelScope.launch {
            when (val result = analyzeExcelFile(file, sheet)) {
                is ApiResult.Success -> {
                    val analysis = result.data
                    setState {
                        copy(
                            isAnalyzing = false,
                            analysis = analysis,
                            chosenSheet = analysis.selectedSheetName,
                        )
                    }
                    if (state.analysis != null || analysis.sheetNames.size <= 1) {
                        setState { copy(isNavigating = true) }
                        sendEffect(UploadEffect.NavigateToMapping(analysis.importJobId))
                    }
                }

                is ApiResult.Error ->
                    setState { copy(isAnalyzing = false, uploadError = result) }
            }
        }
    }
}
