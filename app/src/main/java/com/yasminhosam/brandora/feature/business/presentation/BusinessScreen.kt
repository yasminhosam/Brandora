package com.yasminhosam.brandora.feature.business.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.business.domain.model.BusinessNameError

/**
 * Create-business screen: a 5-field form (name required, the rest optional) that posts to
 * POST /businesses. The name is validated client-side before the call; a backend failure
 * surfaces a generic localized error. On success the ViewModel persists the id and emits
 * [BusinessEffect.BusinessCreated], which the nav host turns into navigation to upload.
 */
@Composable
fun BusinessScreen(
    state: BusinessUiState,
    onEvent: (BusinessEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
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
            text = stringResource(R.string.business_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.business_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        // Business name — required, inline error follows the field.
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(BusinessEvent.NameChanged(it)) },
            label = { Text(text = stringResource(R.string.business_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.nameError != null
        )
        state.nameError?.let { error ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (error) {
                    BusinessNameError.BLANK -> stringResource(R.string.business_name_required)
                    BusinessNameError.TOO_LONG -> stringResource(R.string.business_name_too_long)
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error
            )
        }
        Spacer(Modifier.height(16.dp))

        optionalField(
            label = stringResource(R.string.business_description_label),
            value = state.description,
            onChange = { onEvent(BusinessEvent.DescriptionChanged(it)) },
            maxLines = 2
        )
        Spacer(Modifier.height(16.dp))

        optionalField(
            label = stringResource(R.string.business_category_label),
            value = state.category,
            onChange = { onEvent(BusinessEvent.CategoryChanged(it)) },
            maxLines = 1
        )
        Spacer(Modifier.height(16.dp))

        optionalField(
            label = stringResource(R.string.business_phone_label),
            value = state.phone,
            onChange = { onEvent(BusinessEvent.PhoneChanged(it)) },
            maxLines = 1
        )
        Spacer(Modifier.height(16.dp))

        optionalField(
            label = stringResource(R.string.business_address_label),
            value = state.address,
            onChange = { onEvent(BusinessEvent.AddressChanged(it)) },
            maxLines = 2
        )

        Spacer(Modifier.height(24.dp))

        state.createError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.error
            )
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { onEvent(BusinessEvent.CreateClicked) },
            enabled = !state.isCreating,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isCreating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.business_creating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.business_create),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

/** Optional field: label plus an "Optional" placeholder, never shows an inline error. */
@Composable
private fun optionalField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    maxLines: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(text = label) },
        placeholder = { Text(text = stringResource(R.string.business_optional)) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = maxLines
    )
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun BusinessEmptyPreview() {
    BrandoraTheme {
        BusinessScreen(state = BusinessUiState(), onEvent = {})
    }
}

@Preview(name = "Creating + name error", showBackground = true)
@Composable
private fun BusinessCreatingPreview() {
    BrandoraTheme {
        BusinessScreen(
            state = BusinessUiState(
                name = "متجر التمر",
                description = "A date shop.",
                category = "Groceries",
                phone = "+966 50 000 0000",
                address = "Riyadh, Saudi Arabia",
                isCreating = true,
                nameError = BusinessNameError.TOO_LONG
            ),
            onEvent = {}
        )
    }
}
