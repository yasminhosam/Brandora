package com.yasminhosam.brandora.feature.auth.presentation.reset

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.feature.auth.domain.usecase.ResetPasswordUseCase
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import com.yasminhosam.brandora.feature.auth.domain.validation.validateRegisterPassword
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reset password: app-link token + new password -> POST /auth/reset-password (204).
 * The token enters through TokenArrived (never a route argument) and lives only in this
 * state; the password follows the SAME rules as Register and is never trimmed. On 204
 * the repository has already cleared the local session (all sessions were revoked), the
 * token and password are dropped from state, and the nav host lands on Login. A 400
 * AUTH_RESET_INVALID shows the dedicated invalid-link note; 429/503 runs the cooldown.
 */
@HiltViewModel
class ResetViewModel @Inject constructor(
    private val resetPassword: ResetPasswordUseCase
) : BaseViewModel<ResetUiState, ResetEvent, ResetEffect>(ResetUiState()) {

    private var cooldownJob: Job? = null

    override fun onEvent(event: ResetEvent) {
        when (event) {
            // A newer link supersedes the token the screen already holds, and any
            // stale invalid-link note no longer applies.
            is ResetEvent.TokenArrived -> setState {
                copy(token = event.token, invalidLink = false, submitError = null)
            }
            is ResetEvent.PasswordChanged -> setState {
                copy(
                    password = event.value,
                    passwordError = if (event.value.isBlank()) AuthPasswordError.REQUIRED else null
                )
            }
            ResetEvent.PasswordFocusLost -> setState {
                copy(passwordError = if (password.isBlank()) AuthPasswordError.REQUIRED else passwordError)
            }
            ResetEvent.SubmitClicked -> onSubmit()
        }
    }

    private fun onSubmit() {
        val state = uiState.value
        if (!state.canSubmit) return

        // No token (restored screen without its link): say so instead of sending anything.
        val token = state.token
        if (token == null) {
            setState { copy(invalidLink = true) }
            return
        }

        // Pure domain validation, identical to Register: 12-128 with an upper/lower/digit,
        // NEVER trimmed - leading/trailing spaces are real characters.
        val passwordError = validateRegisterPassword(state.password)
        if (passwordError != null) {
            setState { copy(passwordError = passwordError) }
            return
        }

        setState {
            copy(
                isSubmitting = true,
                submitError = null,
                invalidLink = false,
                passwordError = null
            )
        }
        viewModelScope.launch {
            when (val result = resetPassword(token, state.password)) {
                is ApiResult.Success -> {
                    // Both secrets lived only in memory: drop them the moment the
                    // request no longer needs them.
                    setState { copy(isSubmitting = false, password = "", token = null) }
                    sendEffect(ResetEffect.ResetSucceeded)
                }
                is ApiResult.Error -> onResetError(result)
            }
        }
    }

    private fun onResetError(result: ApiResult.Error) {
        when {
            // The link itself is gone: expired, already used, or superseded.
            result.errorCode == AuthErrorCodes.AUTH_RESET_INVALID -> setState {
                copy(isSubmitting = false, invalidLink = true, submitError = null)
            }

            result.type == ApiErrorType.AUTH_RATE_LIMITED ||
                result.type == ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE -> startCooldown(result)

            else -> setState {
                copy(isSubmitting = false, submitError = result.type, invalidLink = false)
            }
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
