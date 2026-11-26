package com.yasminhosam.brandora.feature.auth.presentation.reset

import com.yasminhosam.brandora.core.base.*
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown

sealed interface ResetEvent : UiEvent {
    data class EmailChanged(val value: String) : ResetEvent

    data class OtpChanged(val value: String) : ResetEvent

    data class PasswordChanged(val value: String) : ResetEvent

    data object PasswordFocusLost : ResetEvent

    data object SubmitClicked : ResetEvent

    data object ResendClicked : ResetEvent
}

// Memory only: never use SavedStateHandle, navigation arguments or rememberSaveable
// for a password/code. Process death asks the owner to enter the code again.
data class ResetUiState(
    val email: String = "",
    val otp: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val isResending: Boolean = false,
    val submitError: ApiErrorType? = null,
    val invalidCode: Boolean = false,
    val otpFormatInvalid: Boolean = false,
    val emailError: AuthEmailError? = null,
    val passwordError: AuthPasswordError? = null,
    val cooldown: AuthCooldown? = null,
    /** Client-side wait after a successful resend; only the resend button is gated by it. */
    val resendCooldown: AuthCooldown? = null,
    val resendAcknowledged: Boolean = false,
) : UiState {
    val busy
        get() = isSubmitting || isResending

    val canSubmit
        get() =
            email.isNotBlank() &&
                otp.isNotBlank() &&
                password.isNotBlank() &&
                !busy &&
                cooldown == null

    val canResend
        get() = email.isNotBlank() && !busy && cooldown == null && resendCooldown == null
}

sealed interface ResetEffect : UiEffect {
    data object ResetSucceeded : ResetEffect
}
