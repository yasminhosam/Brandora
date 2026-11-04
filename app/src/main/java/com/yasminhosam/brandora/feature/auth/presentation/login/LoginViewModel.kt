package com.yasminhosam.brandora.feature.auth.presentation.login

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.feature.auth.domain.usecase.LoginUseCase
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import com.yasminhosam.brandora.feature.auth.domain.validation.normalizeEmail
import com.yasminhosam.brandora.feature.auth.domain.validation.validateEmail
import com.yasminhosam.brandora.feature.auth.domain.validation.validateLoginPassword
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Login: email + password -> /auth/login. Local domain validation runs before any request,
 * a 429/503 cooldown disables submit for Retry-After (or a 30s fallback) seconds, and the
 * password is dropped from state the moment it is no longer needed. On success the
 * repository has persisted the session; the landing route reuses the existing business gate
 * (a known business -> Home, else Business Create), mirroring BrandoraRoot's startup logic.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase,
    private val currentBusinessStore: CurrentBusinessStore
) : BaseViewModel<LoginUiState, LoginEvent, LoginEffect>(LoginUiState()) {

    private var cooldownJob: Job? = null

    override fun onEvent(event: LoginEvent) {
        when (event) {
            // Fresh input invalidates the previous note for that field, and an erased
            // field becomes required again immediately.
            is LoginEvent.EmailChanged -> setState {
                copy(
                    email = event.value,
                    emailError = if (event.value.isBlank()) AuthEmailError.REQUIRED else null,
                    invalidCredentials = false
                )
            }
            is LoginEvent.PasswordChanged -> setState {
                copy(
                    password = event.value,
                    passwordError = if (event.value.isBlank()) AuthPasswordError.REQUIRED else null,
                    invalidCredentials = false
                )
            }
            LoginEvent.EmailFocusLost -> setState {
                copy(emailError = if (email.isBlank()) AuthEmailError.REQUIRED else emailError)
            }
            LoginEvent.PasswordFocusLost -> setState {
                copy(passwordError = if (password.isBlank()) AuthPasswordError.REQUIRED else passwordError)
            }
            LoginEvent.SubmitClicked -> onSubmit()
        }
    }

    private fun onSubmit() {
        val state = uiState.value
        if (!state.canSubmit) return

        // Pure domain validation: an invalid input never reaches the backend.
        val email = normalizeEmail(state.email)
        val emailError = validateEmail(email)
        val passwordError = validateLoginPassword(state.password)
        if (emailError != null || passwordError != null) {
            setState { copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        setState {
            copy(
                isSubmitting = true,
                submitError = null,
                invalidCredentials = false,
                emailError = null,
                passwordError = null
            )
        }
        viewModelScope.launch {
            when (val result = login(email, state.password)) {
                is ApiResult.Success -> {
                    // Persisting happened in the repository; here we only pick the landing
                    // screen, exactly like BrandoraRoot's startup choice.
                    val nextRoute =
                        if (currentBusinessStore.businessId.first() == null) {
                            Routes.BUSINESS_CREATE
                        } else {
                            Routes.HOME
                        }
                    // The password lived only in memory: drop it as soon as the request
                    // no longer needs it.
                    setState { copy(isSubmitting = false, password = "") }
                    sendEffect(LoginEffect.LoginSucceeded(nextRoute))
                }
                is ApiResult.Error -> onLoginError(result)
            }
        }
    }

    private fun onLoginError(result: ApiResult.Error) {
        when {
            result.errorCode == AuthErrorCodes.INVALID_CREDENTIALS -> setState {
                // One generic note: never reveal whether the email exists.
                copy(isSubmitting = false, invalidCredentials = true, submitError = null)
            }

            result.type == ApiErrorType.AUTH_RATE_LIMITED ||
                result.type == ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE -> startCooldown(result)

            else -> setState {
                copy(isSubmitting = false, submitError = result.type, invalidCredentials = false)
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
                invalidCredentials = false,
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
