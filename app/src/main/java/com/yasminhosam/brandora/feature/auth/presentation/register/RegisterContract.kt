package com.yasminhosam.brandora.feature.auth.presentation.register

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown

sealed interface RegisterEvent : UiEvent {
    data class EmailChanged(val value: String) : RegisterEvent
    data class PasswordChanged(val value: String) : RegisterEvent
    data object SubmitClicked : RegisterEvent
    /** An empty field that was actually used shows the required note on focus loss. */
    data object EmailFocusLost : RegisterEvent
    data object PasswordFocusLost : RegisterEvent
}

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val submitError: ApiErrorType? = null,
    /** 409 EMAIL_UNAVAILABLE: the address is taken; shown on the email field. */
    val emailError: AuthEmailError? = null,
    val passwordError: AuthPasswordError? = null,
    /** 429/503 cooldown: submit stays disabled while it ticks down. */
    val cooldown: AuthCooldown? = null
) : UiState {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() &&
            !isSubmitting && cooldown == null
}

sealed interface RegisterEffect : UiEffect {
    /**
     * The account exists and the session is persisted; [nextRoute] reuses the same business
     * gate as login (a fresh account always lands on Business Create).
     */
    data class RegisterSucceeded(val nextRoute: String) : RegisterEffect
}
