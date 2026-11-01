package com.yasminhosam.brandora.feature.auth.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.cooldownMessageRes
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.auth.presentation.fieldMessageRes

/**
 * Register: email + password posted to POST /auth/register (201), mirroring LoginScreen.
 * The password hint states the local rules; 409 EMAIL_UNAVAILABLE shows on the email field;
 * a 429/503 disables submit while the cooldown counts down. The session returned on success
 * is persisted by the repository before the nav host applies the shared business gate.
 */
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onEvent: (RegisterEvent) -> Unit,
    onLoginClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    // The focus observer fires once on composition; only a loss of focus the field actually
    // held counts as a blur, so untouched fields show no required note.
    var emailHeldFocus by remember { mutableStateOf(false) }
    var passwordHeldFocus by remember { mutableStateOf(false) }

    Column(
        // Edge-to-edge: keep content clear of the status and gesture bars.
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.register_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { onEvent(RegisterEvent.EmailChanged(it)) },
            label = { Text(text = stringResource(R.string.login_email_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        emailHeldFocus = true
                    } else if (emailHeldFocus) {
                        emailHeldFocus = false
                        onEvent(RegisterEvent.EmailFocusLost)
                    }
                },
            singleLine = true,
            isError = state.emailError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        state.emailError?.let { error ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(error.fieldMessageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error
            )
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(RegisterEvent.PasswordChanged(it)) },
            label = { Text(text = stringResource(R.string.login_password_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        passwordHeldFocus = true
                    } else if (passwordHeldFocus) {
                        passwordHeldFocus = false
                        onEvent(RegisterEvent.PasswordFocusLost)
                    }
                },
            singleLine = true,
            isError = state.passwordError != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        val passwordError = state.passwordError
        Spacer(Modifier.height(4.dp))
        if (passwordError != null) {
            Text(
                text = stringResource(passwordError.fieldMessageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error
            )
        } else {
            // The local rules stay visible so the password can be composed correctly once.
            Text(
                text = stringResource(R.string.register_password_requirements),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))

        state.submitError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.error
            )
            Spacer(Modifier.height(12.dp))
        }

        state.cooldown?.let { cooldown ->
            Text(
                text = stringResource(cooldown.type.cooldownMessageRes(), cooldown.seconds),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.error
            )
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { onEvent(RegisterEvent.SubmitClicked) },
            // Blank fields, an in-flight request and a running cooldown all disable submit.
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isSubmitting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.register_in_progress),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.register_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        TextButton(
            onClick = onLoginClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(R.string.register_link_login))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun RegisterEmptyPreview() {
    BrandoraTheme {
        RegisterScreen(state = RegisterUiState(), onEvent = {})
    }
}

@Preview(name = "Email taken + cooldown", showBackground = true)
@Composable
private fun RegisterErrorPreview() {
    BrandoraTheme {
        RegisterScreen(
            state = RegisterUiState(
                email = "taken@dates.example",
                password = "Abcdefghijkl1",
                emailError = com.yasminhosam.brandora.feature.auth.domain.validation
                    .AuthEmailError.EMAIL_UNAVAILABLE
            ),
            onEvent = {}
        )
    }
}
