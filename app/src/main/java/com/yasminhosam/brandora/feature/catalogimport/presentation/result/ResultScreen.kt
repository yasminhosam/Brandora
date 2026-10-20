package com.yasminhosam.brandora.feature.catalogimport.presentation.result

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.fieldLabelRes
import com.yasminhosam.brandora.core.ui.executeErrorRes
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.core.ui.validationErrorRes
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteCounts
import com.yasminhosam.brandora.feature.catalogimport.domain.model.FailedRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.RowError
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationRow

@Composable
fun ResultScreen(
    state: ResultUiState,
    onEvent: (ResultEvent) -> Unit,
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
                text = stringResource(R.string.result_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimary
            )
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.errorRes != null && !state.isTerminal -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(state.errorRes),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = scheme.onBackground
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { onEvent(ResultEvent.DoneClicked) }) {
                            Text(stringResource(R.string.result_done))
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
                        StatusHeader(state)
                        Spacer(Modifier.height(8.dp))
                        if (state.isTerminal || state.isStuck) {
                            CountsCard(state.counts)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (state.isFailed) {
                            FailedWarning()
                            Spacer(Modifier.height(8.dp))
                        }
                        if (!state.hasRowDetail && state.isPartial) {
                            Text(
                                text = stringResource(R.string.result_counts_only),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    if (state.failedRows.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.result_failed_rows)) }
                        items(state.failedRows, key = { "failed-${it.rowNumber}" }) { row -> FailedRowCard(row) }
                    }
                    if (state.invalidRows.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.result_invalid_rows)) }
                        items(state.invalidRows, key = { "invalid-${it.rowNumber}" }) { row -> RowCard(row) }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (state.errorRes != null && !state.isTerminal) {
                        // Recoverable error state - offer retry, not a terminal-success action.
                        Text(
                            text = stringResource(state.errorRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onEvent(ResultEvent.RetryClicked) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = stringResource(R.string.action_retry))
                        }
                    } else if (state.isJobGone || state.isFailed) {
                        Text(
                            text = stringResource(R.string.result_start_over_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onEvent(ResultEvent.DoneClicked) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.result_start_over),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (state.isStuck) {
                        // Pending operation - offer to check status rather than start over.
                        Text(
                            text = stringResource(R.string.result_stuck_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onEvent(ResultEvent.CheckStatusClicked) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.result_check_status),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Terminal success (completed or partial) - Done goes home.
                        Button(
                            onClick = { onEvent(ResultEvent.DoneClicked) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.result_done),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeader(state: ResultUiState) {
    val scheme = MaterialTheme.colorScheme
    val (icon, tint) = when {
        state.isCompleted -> Icons.Default.Check to scheme.primary
        state.isPartial -> Icons.Default.Info to scheme.tertiary
        state.isFailed -> Icons.Default.Close to scheme.error
        state.isStuck -> Icons.Default.Info to scheme.tertiary
        else -> Icons.Default.Info to scheme.onSurfaceVariant
    }
    val title = when {
        state.isCompleted -> stringResource(R.string.result_completed_title)
        state.isPartial -> stringResource(R.string.result_partial_title)
        state.isFailed -> stringResource(R.string.result_failed_title)
        state.isStuck -> stringResource(R.string.result_stuck_title)
        else -> stringResource(R.string.result_title)
    }
    val subtitle = when {
        state.isCompleted -> stringResource(R.string.result_completed_subtitle)
        state.isPartial -> stringResource(R.string.result_partial_subtitle)
        state.isFailed -> stringResource(R.string.result_failed_subtitle)
        state.isStuck -> stringResource(R.string.result_stuck_subtitle)
        else -> ""
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(48.dp).background(tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CountsCard(counts: ExecuteCounts) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.result_counts_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            CountRow(stringResource(R.string.result_inserted), counts.inserted, scheme.primary)
            CountRow(stringResource(R.string.result_updated), counts.updated, scheme.tertiary)
            CountRow(stringResource(R.string.result_unchanged), counts.unchanged, scheme.onSurface)
            CountRow(stringResource(R.string.result_failed_count), counts.failed,
                if (counts.failed > 0) scheme.error else scheme.onSurface)
        }
    }
}

@Composable
private fun CountRow(label: String, value: Int, valueColor: androidx.compose.ui.graphics.Color) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun FailedWarning() {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.errorContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = scheme.onErrorContainer, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.result_failed_warning),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onErrorContainer
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = scheme.onBackground,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun RowCard(row: ValidationRow) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.errorContainer.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.validation_row_number, row.rowNumber),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
            if (!row.productName.isNullOrBlank()) {
                Text(row.productName, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            }
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

@Composable
private fun FailedRowCard(row: FailedRow) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.errorContainer.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.validation_row_number, row.rowNumber),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = stringResource(row.code.executeErrorRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultPartialPreview() {
    BrandoraTheme {
        ResultScreen(
            state = ResultUiState(
                isLoading = false,
                status = ImportStatus.COMPLETED_WITH_ERRORS,
                counts = ExecuteCounts(inserted = 1, updated = 1, unchanged = 1, failed = 1),
                invalidRows = listOf(
                    ValidationRow(5, false, errors = listOf(RowError("basePrice", "INVALID_DECIMAL")))
                ),
                failedRows = listOf(
                    FailedRow(6, "EXECUTION_CONFLICT")
                ),
                hasRowDetail = true
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultFailedPreview() {
    BrandoraTheme {
        ResultScreen(
            state = ResultUiState(
                isLoading = false,
                status = ImportStatus.FAILED,
                counts = ExecuteCounts(inserted = 2, updated = 0, unchanged = 0, failed = 3),
                hasRowDetail = false
            ),
            onEvent = {}
        )
    }
}
