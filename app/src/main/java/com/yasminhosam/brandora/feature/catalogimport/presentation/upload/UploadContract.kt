package com.yasminhosam.brandora.feature.catalogimport.presentation.upload

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile

sealed interface UploadEvent : UiEvent {
    data object ScreenEntered : UploadEvent

    data class SheetChosen(val name: String) : UploadEvent

    /** Null picker results are filtered by the screen; only real references arrive here. */
    data class FilePicked(val uri: String) : UploadEvent

    /** Doubles as the retry action after a failed upload. */
    data object ContinueClicked : UploadEvent

    /** First-run only: leaves the import flow without a file. */
    data object SkipClicked : UploadEvent
}

/** Immutable state the Upload screen renders. */
data class UploadUiState(
    val file: ExcelFile? = null,
    /** Local validation only (type, size, unreadable URI): never carries a backend code. */
    val fileError: ApiErrorType? = null,
    /** The Continue button stays enabled so it can be retried. Kept as the whole error so
     * the backend's stable code reaches the message. */
    val uploadError: ApiResult.Error? = null,
    val analysis: com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis? =
        null,
    val chosenSheet: String? = null,
    val isAnalyzing: Boolean = false,
    val isNavigating: Boolean = false,
) : UiState {

    val fileName: String?
        get() = file?.displayName

    val fileSizeBytes: Long?
        get() = file?.sizeBytes

    val canContinue: Boolean
        get() = file != null && fileError == null && !isAnalyzing && !isNavigating &&
            // A multi-sheet workbook must not advance until one sheet has been chosen
            // explicitly (or the backend already named one for us).
            (analysis == null || analysis.sheetNames.size <= 1 || chosenSheet != null)

    /** The VM drops every event while an analysis runs or navigation is committed, so the
     * matching controls must look disabled too instead of swallowing taps silently. */
    val inputsEnabled: Boolean
        get() = !isAnalyzing && !isNavigating

    /** A local rejection outranks a network failure; both resolve to one message here. */
    val visibleErrorRes: Int?
        get() = fileError?.messageRes() ?: uploadError?.messageRes()
}

sealed interface UploadEffect : UiEffect {

    /** [importId] is the navigation argument for the mapping screen. */
    data class NavigateToMapping(val importId: String) : UploadEffect

    /** First-run only: leave the import flow; the host picks the next screen. */
    data object NavigateToNextStep : UploadEffect
}
