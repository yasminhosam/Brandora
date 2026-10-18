package com.yasminhosam.brandora.feature.catalogimport.presentation.validation

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationSummary

sealed interface ValidationEvent : UiEvent {
    data object RetryClicked : ValidationEvent
    data object LoadMoreClicked : ValidationEvent
    data object ContinueClicked : ValidationEvent
    data object FixClicked : ValidationEvent
}

data class ValidationUiState(
    val isLoading: Boolean = true,
    val summary: ValidationSummary? = null,
    val rows: List<ValidationRow> = emptyList(),
    val totalCount: Int = 0,
    val nextOffset: Int? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreErrorRes: Int? = null,
    val errorRes: Int? = null
) : UiState {

    val hasMore: Boolean get() = nextOffset != null

    val isReady: Boolean get() = !isLoading && errorRes == null && summary != null

    /** Import proceeds with the valid subset; blocked only when nothing is importable. */
    val canContinue: Boolean get() = isReady && (summary?.validRows ?: 0) > 0
}

sealed interface ValidationEffect : UiEffect {
    data object NavigateToPreview : ValidationEffect
    data object NavigateToUpload : ValidationEffect
}
