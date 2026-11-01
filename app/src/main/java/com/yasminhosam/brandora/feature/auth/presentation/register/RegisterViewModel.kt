package com.yasminhosam.brandora.feature.auth.presentation.register

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.feature.auth.domain.usecase.RegisterUseCase
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import com.yasminhosam.brandora.feature.auth.domain.validation.normalizeEmail
import com.yasminhosam.brandora.feature.auth.domain.validation.validateEmail
import com.yasminhosam.brandora.feature.auth.domain.validation.validateRegisterPassword
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Register: email + password -> /auth/register (201). Mirrors LoginViewModel: local domain
 * validation (12-128 chars with an upper/lower/digit) runs before any request, 409
 * EMAIL_UNAVAILABLE lands on the email field, 429/503 disables submit during the cooldown,
 * and the password is dropped from state on success. The repository persists the session,
 * so the landing route reuses the same business gate as login.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val register: RegisterUseCase,
    private val currentBusinessStore: CurrentBusinessStore
) : BaseViewModel<RegisterUiState, RegisterEvent, RegisterEffect>(RegisterUiState()) {

    private var cooldownJob: Job? = null

    override fun onEvent(event: RegisterEvent) {
        when (event) {
            // Fresh input invalidates the previous note for that field - including a
            // stale EMAIL_UNAVAILABLE once the email is edited.
            is RegisterEvent.EmailChanged -> setState {
                copy(
                    email = event.value,
                    emailError = if (event.value.isBlank()) AuthEmailError.REQUIRED else null
                )
            }
            is RegisterEvent.PasswordChanged -> setState {
                copy(
                    password = event.value,
                    passwordError = if (event.value.isBlank()) AuthPasswordError.REQUIRED else null
                )
            }
            RegisterEvent.EmailFocusLost -> setState {
                copy(emailError = if (email.isBlank()) AuthEmailError.REQUIRED else emailError)
            }
            RegisterEvent.PasswordFocusLost -> setState {
                copy(passwordError = if (password.isBlank()) AuthPasswordError.REQUIRED else passwordError)
            }
            RegisterEvent.SubmitClicked -> onSubmit()
        }
    }

    private fun onSubmit() {
        val state = uiState.value
        if (!state.canSubmit) return

        // Pure domain validation: an invalid input never reaches the backend.
        val email = normalizeEmail(state.email)
        val emailError = validateEmail(email)
        val passwordError = validateRegisterPassword(state.password)
        if (emailError != null || passwordError != null) {
            setState { copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        setState {
            copy(
                isSubmitting = true,
                submitError = null,
                emailError = null,
                passwordError = null
            )
        }
        viewModelScope.launch {
            when (val result = register(email, state.password)) {
                is ApiResult.Success -> {
                    // Persisting happened in the repository; only the landing screen is
                    // chosen here, exactly like BrandoraRoot's startup choice.
                    val nextRoute =
                        if (currentBusinessStore.businessId.first() == null) {
                            Routes.BUSINESS_CREATE
                        } else {
                            Routes.HOME
                        }
                    // The password lived only in memory: drop it as soon as the request
                    // no longer needs it.
                    setState { copy(isSubmitting = false, password = "") }
                    sendEffect(RegisterEffect.RegisterSucceeded(nextRoute))
                }
                is ApiResult.Error -> onRegisterError(result)
            }
        }
    }

    private fun onRegisterError(result: ApiResult.Error) {
        when {
            // The address is taken: keep it on the email field, not as a generic error.
            result.errorCode == AuthErrorCodes.EMAIL_UNAVAILABLE -> setState {
                copy(
                    isSubmitting = false,
                    emailError = AuthEmailError.EMAIL_UNAVAILABLE,
                    passwordError = null
                )
            }

            result.type == ApiErrorType.AUTH_RATE_LIMITED ||
                result.type == ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE -> startCooldown(result)

            else -> setState {
                copy(isSubmitting = false, submitError = result.type)
            }
        }
    }

    /** Disables submit for Retry-After seconds (30s fallback) with the matching countdown copy. */
    private fun startCooldown(result: ApiResult.Error) {
        val seconds = result.retryAfterSeconds?.takeIf { it > 0 }
            ?: ApiConfig.RATE_LIMIT_FALLBACK_SECONDS
        cooldownJob?.cancel()
        setState {
            copy(
                isSubmitting = false,
                submitError = null,
                cooldown = AuthCooldown(seconds, result.type)
            )
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
