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
import com.yasminhosam.brandora.feature.auth.presentation.AuthCooldownTicker
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import com.yasminhosam.brandora.feature.business.domain.usecase.BusinessSyncResult
import com.yasminhosam.brandora.feature.business.domain.usecase.ReconcileBusinessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Login: email + password -> /auth/login. Local domain validation runs before any request,
 * a 429/503 cooldown disables submit for Retry-After (or a 30s fallback) seconds, and the
 * password is dropped from state the moment it is no longer needed. On success the
 * repository has persisted the session; owned businesses are reconciled with the server
 * before navigating to Home vs Business Create.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase,
    private val currentBusinessStore: CurrentBusinessStore,
    private val reconcileBusiness: ReconcileBusinessUseCase
) : BaseViewModel<LoginUiState, LoginEvent, LoginEffect>(LoginUiState()) {

    private val cooldownTicker =
        AuthCooldownTicker(viewModelScope) { value -> setState { copy(cooldown = value) } }

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
                    val userId = result.data.user.id
                    currentBusinessStore.clearUnlessOwnedBy(userId)
                    when (val sync = reconcileBusiness(userId)) {
                        BusinessSyncResult.HasBusiness -> {
                            setState { copy(isSubmitting = false, password = "") }
                            sendEffect(LoginEffect.LoginSucceeded(Routes.HOME))
                        }
                        BusinessSyncResult.NoBusiness -> {
                            setState { copy(isSubmitting = false, password = "") }
                            sendEffect(LoginEffect.LoginSucceeded(Routes.BUSINESS_CREATE))
                        }
                        is BusinessSyncResult.Error -> {
                            setState { copy(isSubmitting = false, password = "", submitError = sync.error.type) }
                        }
                    }
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
        setState { copy(isSubmitting = false, submitError = null, invalidCredentials = false) }
        cooldownTicker.start(seconds, result.type)
    }
}
