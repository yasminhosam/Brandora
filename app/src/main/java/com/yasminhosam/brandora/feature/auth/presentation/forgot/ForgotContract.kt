package com.yasminhosam.brandora.feature.auth.presentation.forgot

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown

sealed interface ForgotEvent : UiEvent {
    data class EmailChanged(val value: String) : ForgotEvent
    data object SubmitClicked : ForgotEvent
    /** An empty field that was actually used shows the required note on focus loss. */
    data object EmailFocusLost : ForgotEvent
}

data class ForgotUiState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val submitError: ApiErrorType? = null,
    /** 200: the form is replaced by the one generic success copy (never confirms the address). */
    val isSubmitted: Boolean = false,
    val emailError: AuthEmailError? = null,
    /** 429/503 cooldown: submit stays disabled while it ticks down. */
    val cooldown: AuthCooldown? = null
) : UiState {
    val canSubmit: Boolean
        get() = email.isNotBlank() && !isSubmitting && cooldown == null && !isSubmitted
}

/**
 * No effects: success is state (the screen swaps its own content), and "Back to login"
 * is a plain navigation callback from the composable, like the login/register links.
 */
sealed interface ForgotEffect : UiEffect
