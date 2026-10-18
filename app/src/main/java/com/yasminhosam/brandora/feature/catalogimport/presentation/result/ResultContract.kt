package com.yasminhosam.brandora.feature.catalogimport.presentation.result

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteCounts
import com.yasminhosam.brandora.feature.catalogimport.domain.model.FailedRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationRow

sealed interface ResultEvent : UiEvent {
    data object RetryClicked : ResultEvent
    data object DoneClicked : ResultEvent
}

data class ResultUiState(
    val isLoading: Boolean = true,
    val status: ImportStatus? = null,
    val counts: ExecuteCounts = ExecuteCounts(0, 0, 0, 0),
    val invalidRows: List<ValidationRow> = emptyList(),
    val failedRows: List<FailedRow> = emptyList(),
    /** Row-level detail is only present when Execute itself returned; recovery shows counts only. */
    val hasRowDetail: Boolean = false,
    val errorRes: Int? = null,
    val isJobGone: Boolean = false
) : UiState {

    val isCompleted: Boolean get() = status == ImportStatus.COMPLETED

    val isPartial: Boolean get() = status == ImportStatus.COMPLETED_WITH_ERRORS

    val isFailed: Boolean get() = status == ImportStatus.FAILED

    /** A job still IMPORTING after an interruption needs manual investigation; no reset exists. */
    val isStuck: Boolean get() = status == ImportStatus.IMPORTING || status == ImportStatus.VALIDATING

    val isTerminal: Boolean get() = isCompleted || isPartial || isFailed
}

sealed interface ResultEffect : UiEffect {
    data object NavigateToUpload : ResultEffect
}
