package com.yasminhosam.brandora.feature.auth.presentation.reset

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.contentType
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
 * Reset password: token (held in memory by the ViewModel, delivered from the app link)
 * + new password posted to POST /auth/reset-password (204). The password rules are the
 * Register ones; a 400 AUTH_RESET_INVALID shows the dedicated invalid-link note; a
 * 429/503 disables submit while the cooldown counts down. Success is an effect - the nav
 * host clears the stack to Login and shows the confirmation snackbar.
 */
@Composable
fun ResetScreen(
    state: ResetUiState,
    onEvent: (ResetEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    // The focus observer fires once on composition; only a loss of focus the field actually
    // held counts as a blur, so untouched fields show no required note.
    var passwordHeldFocus by remember { mutableStateOf(false) }
    // Tracked here (not in the state) so toggling visibility never reaches the ViewModel.
    var passwordVisible by remember { mutableStateOf(false) }

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
            text = stringResource(R.string.reset_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.reset_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(ResetEvent.PasswordChanged(it)) },
            label = { Text(text = stringResource(R.string.login_password_label)) },
            modifier = Modifier
                .fillMaxWidth()
                // A NEW password is being chosen, so the provider can suggest one and
                // offer to save it with the account once the reset succeeds.
                .contentType(ContentType.NewPassword)
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        passwordHeldFocus = true
                    } else if (passwordHeldFocus) {
                        passwordHeldFocus = false
                        onEvent(ResetEvent.PasswordFocusLost)
                    }
                },
            singleLine = true,
            isError = state.passwordError != null,
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
            // Single-field screen: the keyboard's Go key IS the submit button.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { onEvent(ResetEvent.SubmitClicked) }),
            // Always occupies one line (nbsp when there is no message) so the field's
            // footprint never changes when an error appears or disappears, and the
            // message sits in the slot TalkBack announces with isError.
            supportingText = {
                Text(
                    text = state.passwordError?.let { stringResource(it.fieldMessageRes()) }
                        ?: "\u00A0",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error
                )
            }
        )
        // The local rules stay visible even while an error is shown, so the password can
        // be composed correctly on the next attempt.
        Text(
            text = stringResource(R.string.register_password_requirements),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        // About the LINK, not a field: expired, used, superseded - or the screen was
        // restored without its token. A newer link clears this note.
        if (state.invalidLink) {
            Text(
                text = stringResource(R.string.reset_invalid_link),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.error
            )
            Spacer(Modifier.height(12.dp))
        }

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
            onClick = { onEvent(ResetEvent.SubmitClicked) },
            // Blank field, an in-flight request and a running cooldown all disable submit.
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
                        text = stringResource(R.string.reset_in_progress),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.reset_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun ResetEmptyPreview() {
    BrandoraTheme {
        ResetScreen(state = ResetUiState(), onEvent = {})
    }
}

@Preview(name = "Invalid link + cooldown", showBackground = true)
@Composable
private fun ResetInvalidPreview() {
    BrandoraTheme {
        ResetScreen(
            state = ResetUiState(
                token = "dead",
                password = "Abcdefghijkl1",
                invalidLink = true,
                cooldown = com.yasminhosam.brandora.core.network.ApiErrorType.AUTH_RATE_LIMITED.let {
                    com.yasminhosam.brandora.feature.auth.presentation.AuthCooldown(12, it)
                }
            ),
            onEvent = {}
        )
    }
}
