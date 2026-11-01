package com.yasminhosam.brandora.feature.auth.presentation.login

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown

sealed interface LoginEvent : UiEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object SubmitClicked : LoginEvent
    /** An empty field that was actually used shows the required note on focus loss. */
    data object EmailFocusLost : LoginEvent
    data object PasswordFocusLost : LoginEvent
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val submitError: ApiErrorType? = null,
    /** 401 INVALID_CREDENTIALS: wrong email/password, shown inline instead of the generic error. */
    val invalidCredentials: Boolean = false,
    val emailError: AuthEmailError? = null,
    val passwordError: AuthPasswordError? = null,
    /** 429/503 cooldown: submit stays disabled while it ticks down. */
    val cooldown: AuthCooldown? = null
) : UiState {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() &&
            !isSubmitting && cooldown == null
}

sealed interface LoginEffect : UiEffect {
    /**
     * The session is already persisted; [nextRoute] reuses the existing business gate:
     * a known business lands on Home, otherwise Business Create comes next.
     */
    data class LoginSucceeded(val nextRoute: String) : LoginEffect
}
