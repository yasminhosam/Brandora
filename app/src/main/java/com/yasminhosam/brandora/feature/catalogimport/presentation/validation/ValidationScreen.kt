package com.yasminhosam.brandora.feature.catalogimport.presentation.validation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.fieldLabelRes
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.core.ui.validationErrorRes
import com.yasminhosam.brandora.feature.catalogimport.domain.model.RowError
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationSummary

@Composable
fun ValidationScreen(
    state: ValidationUiState,
    onEvent: (ValidationEvent) -> Unit,
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.primary, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.validation_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimary
            )
            Text(
                text = stringResource(R.string.validation_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onPrimary.copy(alpha = 0.85f)
            )
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.errorRes != null && !state.isReady -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(state.errorRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onBackground
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { onEvent(ValidationEvent.RetryClicked) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        state.summary?.let { SummaryCard(it, state.totalCount) }
                        Spacer(Modifier.height(8.dp))
                    }
                    items(state.rows, key = { it.rowNumber }) { row -> RowItem(row) }
                    if (state.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    } else if (state.hasMore) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = { onEvent(ValidationEvent.LoadMoreClicked) }) {
                                    Text(stringResource(R.string.validation_load_more))
                                }
                            }
                        }
                    } else if (state.loadMoreErrorRes != null) {
                        item {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(state.loadMoreErrorRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.error
                                )
                                TextButton(onClick = { onEvent(ValidationEvent.LoadMoreClicked) }) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    val summary = state.summary
                    if (summary != null && summary.invalidRows > 0 && state.canContinue) {
                        Text(
                            text = stringResource(R.string.validation_has_invalid, summary.invalidRows),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.tertiary
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (summary != null && summary.validRows == 0) {
                        Text(
                            text = stringResource(R.string.validation_none_valid),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = { onEvent(ValidationEvent.ContinueClicked) },
                        enabled = state.canContinue,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.validation_continue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onEvent(ValidationEvent.FixClicked) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.validation_fix))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: ValidationSummary, totalCount: Int) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.validation_summary_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            SummaryRow(stringResource(R.string.validation_total), totalCount.toString())
            SummaryRow(
                stringResource(R.string.validation_valid),
                summary.validRows.toString(),
                scheme.primary
            )
            SummaryRow(
                stringResource(R.string.validation_invalid),
                summary.invalidRows.toString(),
                if (summary.invalidRows > 0) scheme.error else scheme.onSurface
            )
            if (summary.skippedRows > 0) {
                SummaryRow(stringResource(R.string.validation_skipped), summary.skippedRows.toString())
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun RowItem(row: ValidationRow) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (row.isValid) scheme.surfaceContainerLow else scheme.errorContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(if (row.isValid) scheme.primary else scheme.error, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (row.isValid) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (row.isValid) scheme.onPrimary else scheme.onError,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = stringResource(R.string.validation_row_number, row.rowNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
                if (row.isValid) {
                    if (!row.productName.isNullOrBlank()) {
                        Text(
                            text = row.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface
                        )
                    }
                } else {
                    row.errors.forEach { error ->
                        val field = stringResource(error.field.fieldLabelRes())
                        val message = stringResource(error.code.validationErrorRes())
                        Text(
                            text = stringResource(R.string.validation_error_line, field, message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.error
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ValidationPreview() {
    BrandoraTheme {
        ValidationScreen(
            state = ValidationUiState(
                isLoading = false,
                summary = ValidationSummary(totalRows = 5, validRows = 3, invalidRows = 2, skippedRows = 0),
                totalCount = 5,
                nextOffset = null,
                rows = listOf(
                    ValidationRow(2, true, productName = "قميص"),
                    ValidationRow(3, true, productName = "قبعة"),
                    ValidationRow(4, false, errors = listOf(
                        RowError("basePrice", "INVALID_DECIMAL"),
                        RowError("name", "REQUIRED")
                    )),
                    ValidationRow(5, true, productName = "حقيبة")
                )
            ),
            onEvent = {}
        )
    }
}
