package com.yasminhosam.brandora.feature.auth.presentation.forgot

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.cooldownMessageRes
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.auth.presentation.AuthBrandHeader
import com.yasminhosam.brandora.feature.auth.presentation.fieldMessageRes

/**
 * Forgot password: email posted to POST /auth/forgot-password. On 200 the form is
 * replaced by the one generic success copy - identical for known and unknown addresses,
 * so the screen never confirms whether an account exists - plus a way back to Login.
 * A 429/503 disables submit while the cooldown counts down; the 503 with
 * AUTH_EMAIL_UNAVAILABLE shows its own typed message instead.
 */
@Composable
fun ForgotScreen(
    state: ForgotUiState,
    onEvent: (ForgotEvent) -> Unit,
    onBackToLoginClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    // The focus observer fires once on composition; only a loss of focus the field actually
    // held counts as a blur, so untouched fields show no required note.
    var emailHeldFocus by remember { mutableStateOf(false) }

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
            text = stringResource(R.string.forgot_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.forgot_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        if (state.isSubmitted) {
            // One generic message, then a way back; the form is gone so the request
            // cannot be spammed from this state.
            Text(
                text = stringResource(R.string.forgot_success),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onBackground
            )
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = onBackToLoginClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(R.string.forgot_link_login))
            }
        } else {
            OutlinedTextField(
                value = state.email,
                onValueChange = { onEvent(ForgotEvent.EmailChanged(it)) },
                label = { Text(text = stringResource(R.string.login_email_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (focus.isFocused) {
                            emailHeldFocus = true
                        } else if (emailHeldFocus) {
                            emailHeldFocus = false
                            onEvent(ForgotEvent.EmailFocusLost)
                        }
                    },
                singleLine = true,
                isError = state.emailError != null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    // Single-field screen: the keyboard's Go key IS the submit button.
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { onEvent(ForgotEvent.SubmitClicked) }),
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
            Spacer(Modifier.height(8.dp))

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
                onClick = { onEvent(ForgotEvent.SubmitClicked) },
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
                            text = stringResource(R.string.forgot_in_progress),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.forgot_action),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            TextButton(
                onClick = onBackToLoginClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(R.string.forgot_link_login))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun ForgotEmptyPreview() {
    BrandoraTheme {
        ForgotScreen(state = ForgotUiState(), onEvent = {})
    }
}

@Preview(name = "Submitted", showBackground = true)
@Composable
private fun ForgotSubmittedPreview() {
    BrandoraTheme {
        ForgotScreen(state = ForgotUiState(email = "shop@dates.example", isSubmitted = true), onEvent = {})
    }
}
