package com.yasminhosam.brandora.feature.auth.presentation.reset

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.auth.presentation.AuthBrandHeader
import com.yasminhosam.brandora.feature.auth.presentation.fieldMessageRes

@Composable
fun ResetScreen(state: ResetUiState, onEvent: (ResetEvent) -> Unit, modifier: Modifier = Modifier) {
    // Only a focus that was actually held counts as lost, so the field does not validate on
    // the first composition (same pattern as Login/Register/Forgot).
    var passwordHeldFocus by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AuthBrandHeader()
        Text(stringResource(R.string.otp_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.otp_instructions))
        OutlinedTextField(
            state.email,
            { onEvent(ResetEvent.EmailChanged(it)) },
            label = { Text(stringResource(R.string.login_email_label)) },
            singleLine = true,
            enabled = !state.busy,
            isError = state.emailError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        state.emailError?.let {
            Text(stringResource(it.fieldMessageRes()), color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(
            state.otp,
            { onEvent(ResetEvent.OtpChanged(it)) },
            label = { Text(stringResource(R.string.otp_label)) },
            singleLine = true,
            enabled = !state.busy,
            isError = state.otpFormatInvalid || state.invalidCode,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.otpFormatInvalid)
            Text(stringResource(R.string.otp_format_error), color = MaterialTheme.colorScheme.error)
        if (state.invalidCode)
            Text(
                stringResource(R.string.otp_invalid_error),
                color = MaterialTheme.colorScheme.error,
            )
        OutlinedTextField(
            state.password,
            { onEvent(ResetEvent.PasswordChanged(it)) },
            label = { Text(stringResource(R.string.otp_new_password)) },
            singleLine = true,
            enabled = !state.busy,
            isError = state.passwordError != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier =
                Modifier.fillMaxWidth().onFocusChanged { focus ->
                    if (focus.isFocused) {
                        passwordHeldFocus = true
                    } else if (passwordHeldFocus) {
                        passwordHeldFocus = false
                        onEvent(ResetEvent.PasswordFocusLost)
                    }
                },
        )
        state.passwordError?.let {
            Text(stringResource(it.fieldMessageRes()), color = MaterialTheme.colorScheme.error)
        }
        state.submitError?.let {
            Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error)
        }
        // One wait line at a time: a rate limit outranks the resend countdown.
        val waitSeconds = state.cooldown?.seconds ?: state.resendCooldown?.seconds
        if (waitSeconds != null) Text(stringResource(R.string.otp_wait, waitSeconds))
        if (state.resendAcknowledged) Text(stringResource(R.string.forgot_success))
        Button(
            { onEvent(ResetEvent.SubmitClicked) },
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.otp_reset))
        }
        TextButton({ onEvent(ResetEvent.ResendClicked) }, enabled = state.canResend) {
            Text(stringResource(R.string.otp_resend))
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
