package com.yasminhosam.brandora.feature.auth.presentation.forgot

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.auth.domain.usecase.ForgotPasswordUseCase
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.domain.validation.normalizeEmail
import com.yasminhosam.brandora.feature.auth.domain.validation.validateEmail
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Forgot password: email -> POST /auth/forgot-password. Local domain validation runs
 * before any request, and a 200 ALWAYS shows the same generic copy - the reply never
 * confirms whether the address exists. A 429/503 disables submit for Retry-After (or a
 * 30s fallback) seconds; the 503 AUTH_EMAIL_UNAVAILABLE is a plain typed error (no
 * Retry-After in that contract), so the button just becomes usable again.
 */
@HiltViewModel
class ForgotViewModel @Inject constructor(
    private val forgotPassword: ForgotPasswordUseCase
) : BaseViewModel<ForgotUiState, ForgotEvent, ForgotEffect>(ForgotUiState()) {

    private var cooldownJob: Job? = null

    override fun onEvent(event: ForgotEvent) {
        when (event) {
            // Fresh input invalidates the previous note, and an erased field becomes
            // required again immediately.
            is ForgotEvent.EmailChanged -> setState {
                copy(
                    email = event.value,
                    emailError = if (event.value.isBlank()) AuthEmailError.REQUIRED else null
                )
            }
            ForgotEvent.EmailFocusLost -> setState {
                copy(emailError = if (email.isBlank()) AuthEmailError.REQUIRED else emailError)
            }
            ForgotEvent.SubmitClicked -> onSubmit()
        }
    }

    private fun onSubmit() {
        val state = uiState.value
        if (!state.canSubmit) return

        // Pure domain validation: an invalid input never reaches the backend.
        val email = normalizeEmail(state.email)
        val emailError = validateEmail(email)
        if (emailError != null) {
            setState { copy(emailError = emailError) }
            return
        }

        setState { copy(isSubmitting = true, submitError = null, emailError = null) }
        viewModelScope.launch {
            when (val result = forgotPassword(email)) {
                // Success: the SAME copy for every address, delivered or not.
                is ApiResult.Success ->
                    setState { copy(isSubmitting = false, isSubmitted = true) }
                is ApiResult.Error -> onForgotError(result)
            }
        }
    }

    private fun onForgotError(result: ApiResult.Error) {
        when {
            result.type == ApiErrorType.AUTH_RATE_LIMITED ||
                result.type == ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE -> startCooldown(result)

            else -> setState { copy(isSubmitting = false, submitError = result.type) }
        }
    }

    /** Disables submit for Retry-After seconds (30s fallback) with the matching countdown copy. */
    private fun startCooldown(result: ApiResult.Error) {
        val seconds = result.retryAfterSeconds?.takeIf { it > 0 }
            ?: ApiConfig.RATE_LIMIT_FALLBACK_SECONDS
        cooldownJob?.cancel()
        setState {
            copy(isSubmitting = false, submitError = null, cooldown = AuthCooldown(seconds, result.type))
        }
        cooldownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                remaining--
                setState { copy(cooldown = cooldown?.copy(seconds = remaining)) }
            }
            setState { copy(cooldown = null) }
        }
    }
}
