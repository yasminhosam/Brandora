package com.yasminhosam.brandora.feature.auth.presentation.reset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.*
import com.yasminhosam.brandora.feature.auth.domain.usecase.*
import com.yasminhosam.brandora.feature.auth.domain.validation.*
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldownTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*

@HiltViewModel
class ResetViewModel
@Inject
constructor(
    private val resetPassword: ResetPasswordUseCase,
    private val forgotPassword: ForgotPasswordUseCase,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : BaseViewModel<ResetUiState, ResetEvent, ResetEffect>(
    ResetUiState(email = savedStateHandle.get<String>("email").orEmpty())
) {
    private val cooldownTicker =
        AuthCooldownTicker(viewModelScope) { value -> setState { copy(cooldown = value) } }

    /** Waits between resends; only [ResetUiState.canResend] is gated by this one. */
    private val resendTicker =
        AuthCooldownTicker(viewModelScope) { value -> setState { copy(resendCooldown = value) } }

    override fun onEvent(event: ResetEvent) {
        if (uiState.value.busy) return
        when (event) {
            is ResetEvent.EmailChanged ->
                setState {
                    copy(
                        email = event.value,
                        emailError = null,
                        invalidCode = false,
                        resendAcknowledged = false,
                    )
                }
            is ResetEvent.OtpChanged ->
                setState {
                    copy(otp = event.value.take(128), otpFormatInvalid = false, invalidCode = false)
                }
            is ResetEvent.PasswordChanged ->
                setState { copy(password = event.value, passwordError = null) }
            ResetEvent.PasswordFocusLost ->
                setState { copy(passwordError = validateRegisterPassword(password)) }
            ResetEvent.SubmitClicked -> submit()
            ResetEvent.ResendClicked -> resend()
        }
    }

    private fun submit() {
        val state = uiState.value
        if (!state.canSubmit) return
        val email = normalizeEmail(state.email)
        val emailError = validateEmail(email)
        val passwordError = validateRegisterPassword(state.password)
        val badCode = !Regex("[0-9]{8}").matches(state.otp)
        setState {
            copy(emailError = emailError, passwordError = passwordError, otpFormatInvalid = badCode)
        }
        if (emailError != null || passwordError != null || badCode) return
        setState { copy(isSubmitting = true, submitError = null, invalidCode = false) }
        viewModelScope.launch {
            when (val result = resetPassword(email, state.otp, state.password)) {
                is ApiResult.Success -> {
                    setState { copy(isSubmitting = false, password = "", otp = "") }
                    sendEffect(ResetEffect.ResetSucceeded)
                }
                is ApiResult.Error -> failure(result)
            }
        }
    }

    private fun resend() {
        val state = uiState.value
        if (!state.canResend) return
        val email = normalizeEmail(state.email)
        val error = validateEmail(email)
        if (error != null) {
            setState { copy(emailError = error) }
            return
        }
        setState { copy(isResending = true, submitError = null, resendAcknowledged = false) }
        // Explicit tap only. A network timeout never triggers a second email.
        viewModelScope.launch {
            when (val result = forgotPassword(email)) {
                is ApiResult.Success -> {
                    setState {
                        copy(
                            isResending = false,
                            otp = "",
                            invalidCode = false,
                            resendAcknowledged = true,
                        )
                    }
                    // Resend wait: gates resend only, so a code the user already has can
                    // still be submitted while this counts down.
                    resendTicker.start(
                        ApiConfig.RATE_LIMIT_FALLBACK_SECONDS,
                        ApiErrorType.AUTH_RATE_LIMITED,
                    )
                }
                is ApiResult.Error -> failure(result)
            }
        }
    }

    private fun failure(result: ApiResult.Error) {
        val rateLimited =
            result.type == ApiErrorType.AUTH_RATE_LIMITED ||
                result.type == ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE
        setState {
            copy(
                isSubmitting = false,
                isResending = false,
                // One message at a time: a rate limit is told by its countdown, an invalid
                // code by the field note, anything else by its typed line - and any failure
                // supersedes the stale "code sent" acknowledgment.
                submitError =
                    when {
                        rateLimited -> null
                        result.errorCode == AuthErrorCodes.AUTH_RESET_INVALID -> null
                        else -> result.type
                    },
                invalidCode = result.errorCode == AuthErrorCodes.AUTH_RESET_INVALID,
                resendAcknowledged = false,
            )
        }
        if (rateLimited)
            cooldownTicker.start(
                result.retryAfterSeconds?.takeIf { it > 0 }
                    ?: ApiConfig.RATE_LIMIT_FALLBACK_SECONDS,
                result.type,
            )
    }
}
