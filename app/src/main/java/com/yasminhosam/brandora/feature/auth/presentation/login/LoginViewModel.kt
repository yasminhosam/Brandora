package com.yasminhosam.brandora.feature.auth.presentation.login

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.feature.auth.domain.usecase.LoginUseCase
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Minimal login: email + password -> /auth/login. On success the repository has already
 * persisted the session; the landing route reuses the existing business gate (a known
 * business -> Home, else Business Create), mirroring BrandoraRoot's startup logic.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase,
    private val currentBusinessStore: CurrentBusinessStore
) : BaseViewModel<LoginUiState, LoginEvent, LoginEffect>(LoginUiState()) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            // A fresh edit invalidates the previous "wrong credentials" note.
            is LoginEvent.EmailChanged -> setState {
                copy(email = event.value, invalidCredentials = false)
            }
            is LoginEvent.PasswordChanged -> setState {
                copy(password = event.value, invalidCredentials = false)
            }
            LoginEvent.SubmitClicked -> onSubmit()
        }
    }

    private fun onSubmit() {
        val state = uiState.value
        if (!state.canSubmit) return
        setState { copy(isSubmitting = true, submitError = null, invalidCredentials = false) }
        viewModelScope.launch {
            when (val result = login(state.email, state.password)) {
                is ApiResult.Success -> {
                    // Persisting happened in the repository; here we only pick the landing
                    // screen, exactly like BrandoraRoot's startup choice.
                    val nextRoute =
                        if (currentBusinessStore.businessId.first() == null) {
                            Routes.BUSINESS_CREATE
                        } else {
                            Routes.HOME
                        }
                    setState { copy(isSubmitting = false) }
                    sendEffect(LoginEffect.LoginSucceeded(nextRoute))
                }
                is ApiResult.Error -> setState {
                    copy(
                        isSubmitting = false,
                        invalidCredentials = result.errorCode == AuthErrorCodes.INVALID_CREDENTIALS,
                        submitError = if (result.errorCode == AuthErrorCodes.INVALID_CREDENTIALS) {
                            null // the inline credentials message replaces the generic one
                        } else {
                            result.type
                        }
                    )
                }
            }
        }
    }
}
