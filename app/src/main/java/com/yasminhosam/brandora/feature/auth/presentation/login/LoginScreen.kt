package com.yasminhosam.brandora.feature.auth.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.contentType
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.cooldownMessageRes
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.auth.presentation.AuthBrandHeader
import com.yasminhosam.brandora.feature.auth.presentation.fieldMessageRes

/**
 * Login: email + password posted to POST /auth/login, with local validation shown under
 * each field (required on blur/erase, syntax on submit). On success the nav host reuses
 * the existing business gate (businessId present -> Home, else Business Create). A 401
 * INVALID_CREDENTIALS shows the inline message instead of the generic error; a 429/503
 * disables submit while the cooldown counts down.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    // The focus observer fires once on composition; only a loss of focus the field actually
    // held counts as a blur, so untouched fields show no required note.
    var emailHeldFocus by remember { mutableStateOf(false) }
    var passwordHeldFocus by remember { mutableStateOf(false) }
    // Tracked here (not in the state) so toggling visibility never reaches the ViewModel.
    var passwordVisible by remember { mutableStateOf(false) }
    // Lets the keyboard's Next key jump from email to password.
    val passwordFocusRequester = remember { FocusRequester() }

    Column(
        // Edge-to-edge: keep content clear of the status, gesture and keyboard areas;
        // verticalScroll keeps the submit button reachable at large font sizes.
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AuthBrandHeader()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
            label = { Text(text = stringResource(R.string.login_email_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .contentType(ContentType.EmailAddress)
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        emailHeldFocus = true
                    } else if (emailHeldFocus) {
                        emailHeldFocus = false
                        onEvent(LoginEvent.EmailFocusLost)
                    }
                },
            singleLine = true,
            isError = state.invalidCredentials || state.emailError != null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
            // Always occupies one line (nbsp when there is no message) so the field's
            // footprint never changes when an error appears or disappears, and the
            // message sits in the slot TalkBack announces with isError.
            supportingText = {
                Text(
                    text = state.emailError?.let { stringResource(it.fieldMessageRes()) }
                        ?: "\u00A0",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error
                )
            }
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
            label = { Text(text = stringResource(R.string.login_password_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
                .contentType(ContentType.Password)
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        passwordHeldFocus = true
                    } else if (passwordHeldFocus) {
                        passwordHeldFocus = false
                        onEvent(LoginEvent.PasswordFocusLost)
                    }
                },
            singleLine = true,
            isError = state.invalidCredentials || state.passwordError != null,
            visualTransformation =
                if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            if (passwordVisible) {
                                R.drawable.ic_visibility_off
                            } else {
                                R.drawable.ic_visibility
                            }
                        ),
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.password_hide else R.string.password_show
                        ),
                        tint = scheme.onSurfaceVariant
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { onEvent(LoginEvent.SubmitClicked) }),
            supportingText = {
                Text(
                    text = state.passwordError?.let { stringResource(it.fieldMessageRes()) }
                        ?: "\u00A0",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error
                )
            }
        )

        if (state.invalidCredentials) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.login_invalid_credentials),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error
            )
        }

        // Recovery sits directly under the password field, before the submit button.
        TextButton(
            onClick = onForgotPasswordClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(R.string.login_link_forgot))
        }
        Spacer(Modifier.height(4.dp))

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
            onClick = { onEvent(LoginEvent.SubmitClicked) },
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
                        text = stringResource(R.string.login_in_progress),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.login_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        TextButton(
            onClick = onRegisterClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(R.string.login_link_register))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun LoginEmptyPreview() {
    BrandoraTheme {
        LoginScreen(state = LoginUiState(), onEvent = {})
    }
}

@Preview(name = "Submitting + invalid credentials", showBackground = true)
@Composable
private fun LoginSubmittingPreview() {
    BrandoraTheme {
        LoginScreen(
            state = LoginUiState(
                email = "shop@dates.example",
                password = "secret",
                isSubmitting = true,
                invalidCredentials = true
            ),
            onEvent = {}
        )
    }
}
