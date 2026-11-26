package com.yasminhosam.brandora.feature.home.presentation

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState

sealed interface HomeEvent : UiEvent {
    /** Sent every time the screen resumes so the badge is fresh after visiting the chats. */
    data object Refresh : HomeEvent
}

data class HomeUiState(
    /** Number of open chats; null until known or when the request failed (no badge then). */
    val openChatCount: Int? = null
) : UiState

/** Navigation is handled by callbacks in the screen; Home has no one-shot effects yet. */
sealed interface HomeEffect : UiEffect
