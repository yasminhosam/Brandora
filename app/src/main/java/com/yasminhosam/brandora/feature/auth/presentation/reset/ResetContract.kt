package com.yasminhosam.brandora.feature.auth.presentation.reset

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown

sealed interface ResetEvent : UiEvent {
    /** The app-link token arrived (or a newer link superseded the one already held). */
    data class TokenArrived(val token: String) : ResetEvent
    data class PasswordChanged(val value: String) : ResetEvent
    data object SubmitClicked : ResetEvent
    /** An empty field that was actually used shows the required note on focus loss. */
    data object PasswordFocusLost : ResetEvent
}

data class ResetUiState(
    /**
     * The app-link token, memory-only: never a route argument, never persisted, cleared
     * on success, and dropped with the ViewModel when the screen leaves the stack.
     */
    val token: String? = null,
    val password: String = "",
    val isSubmitting: Boolean = false,
    val submitError: ApiErrorType? = null,
    /** 400 AUTH_RESET_INVALID (or no token at all): the link is expired/used/invalid. */
    val invalidLink: Boolean = false,
    val passwordError: AuthPasswordError? = null,
    /** 429/503 cooldown: submit stays disabled while it ticks down. */
    val cooldown: AuthCooldown? = null
) : UiState {
    // Deliberately not gated on the token: a tokenless screen (process restored without
    // its link) still submits and gets the invalid-link note instead of a dead button.
    val canSubmit: Boolean
        get() = password.isNotBlank() && !isSubmitting && cooldown == null
}

sealed interface ResetEffect : UiEffect {
    /** 204: the repository cleared the local session; the nav host clears the stack to Login. */
    data object ResetSucceeded : ResetEffect
}
