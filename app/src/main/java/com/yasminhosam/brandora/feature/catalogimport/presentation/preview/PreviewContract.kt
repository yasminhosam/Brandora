package com.yasminhosam.brandora.feature.catalogimport.presentation.preview

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewItem
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewSummary

sealed interface PreviewEvent : UiEvent {
    data object RetryClicked : PreviewEvent
    data object LoadMoreClicked : PreviewEvent
    data object ImportClicked : PreviewEvent
    data object BackToValidation : PreviewEvent
}

data class PreviewUiState(
    val isLoading: Boolean = true,
    val summary: PreviewSummary? = null,
    val items: List<PreviewItem> = emptyList(),
    val totalCount: Int = 0,
    val nextOffset: Int? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreErrorRes: Int? = null,
    val errorRes: Int? = null
) : UiState {

    val hasMore: Boolean get() = nextOffset != null

    val isReady: Boolean get() = !isLoading && errorRes == null && summary != null
}

sealed interface PreviewEffect : UiEffect {
    data class NavigateToExecute(val importId: String) : PreviewEffect
    data object NavigateBack : PreviewEffect
}
