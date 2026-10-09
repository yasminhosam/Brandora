package com.yasminhosam.brandora.feature.catalogimport.presentation.upload

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile

sealed interface UploadEvent : UiEvent {

    /** Null picker results are filtered by the screen; only real references arrive here. */
    data class FilePicked(val uri: String) : UploadEvent

    /** Doubles as the retry action after a failed upload. */
    data object ContinueClicked : UploadEvent

    data object ManualEntryClicked : UploadEvent
}

/** Immutable state the Upload screen renders. */
data class UploadUiState(
    val file: ExcelFile? = null,
    val fileError: ApiErrorType? = null,
    /** The Continue button stays enabled so it can be retried. */
    val uploadError: ApiErrorType? = null,
    val isAnalyzing: Boolean = false
) : UiState {

    val fileName: String? get() = file?.displayName

    val fileSizeBytes: Long? get() = file?.sizeBytes

    val canContinue: Boolean get() = file != null && fileError == null && !isAnalyzing

    /** A local rejection outranks a network failure. */
    val visibleError: ApiErrorType? get() = fileError ?: uploadError
}

sealed interface UploadEffect : UiEffect {

    /** [importJobId] is the navigation argument for the review screen. */
    data class NavigateToReview(val importJobId: String) : UploadEffect

    data object NavigateToManualEntry : UploadEffect
}
