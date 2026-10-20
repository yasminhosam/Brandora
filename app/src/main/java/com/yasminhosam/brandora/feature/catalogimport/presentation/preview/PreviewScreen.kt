package com.yasminhosam.brandora.feature.catalogimport.presentation.preview

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.yasminhosam.brandora.feature.catalogimport.domain.model.FieldChange
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewAction
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewItem
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewSummary
import com.yasminhosam.brandora.feature.catalogimport.domain.model.RowError

@Composable
fun PreviewScreen(
    state: PreviewUiState,
    onEvent: (PreviewEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    // Backend-computed truth: nothing will match the existing catalog, so every valid row
    // becomes a new product. Confirm before executing — same rule from every entry point.
    val allNewOnly = state.summary?.let { it.willUpdate == 0 && it.willInsert > 0 } == true
    val showAllNewConfirm = remember { mutableStateOf(false) }
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onEvent(PreviewEvent.BackToValidation) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.mapping_back),
                        tint = scheme.onPrimary
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.preview_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary
                )
            }
            Text(
                text = stringResource(R.string.preview_subtitle),
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
                        Button(onClick = { onEvent(PreviewEvent.RetryClicked) }) {
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
                        Spacer(Modifier.height(12.dp))
                        AdvisoryBanner()
                        Spacer(Modifier.height(8.dp))
                        state.summary?.let { SummaryCard(it, state.totalCount) }
                        // Warn when every valid row will be inserted as a new product (nothing will
                        // be updated). Only meaningful when there is something to insert.
                        state.summary?.let { summary ->
                            if (summary.willUpdate == 0 && summary.willInsert > 0) {
                                Spacer(Modifier.height(8.dp))
                                AllNewWarning()
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    items(state.items, key = { it.rowNumber }) { item -> PreviewRow(item) }
                    if (state.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    } else if (state.hasMore) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = { onEvent(PreviewEvent.LoadMoreClicked) }) {
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
                                TextButton(onClick = { onEvent(PreviewEvent.LoadMoreClicked) }) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(R.string.preview_execute_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (allNewOnly) {
                                showAllNewConfirm.value = true
                            } else {
                                onEvent(PreviewEvent.ImportClicked)
                            }
                        },
                        enabled = state.isReady,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.preview_import),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showAllNewConfirm.value) {
        AllNewConfirmDialog(
            insertCount = state.summary?.willInsert ?: 0,
            onDismiss = { showAllNewConfirm.value = false },
            onConfirm = {
                showAllNewConfirm.value = false
                onEvent(PreviewEvent.ImportClicked)
            }
        )
    }
}

@Composable
private fun AdvisoryBanner() {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.tertiaryContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = scheme.onTertiaryContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.preview_advisory),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onTertiaryContainer
        )
    }
}

@Composable
private fun SummaryCard(summary: PreviewSummary, totalCount: Int) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.preview_summary_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            SummaryRow(stringResource(R.string.validation_total), totalCount.toString())
            SummaryRow(stringResource(R.string.preview_will_insert), summary.willInsert.toString(), scheme.primary)
            SummaryRow(stringResource(R.string.preview_will_update), summary.willUpdate.toString(), scheme.tertiary)
            SummaryRow(stringResource(R.string.preview_unchanged), summary.unchanged.toString())
            if (summary.invalidRows > 0) {
                SummaryRow(stringResource(R.string.validation_invalid), summary.invalidRows.toString(), scheme.error)
            }
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
private fun PreviewRow(item: PreviewItem) {
    val scheme = MaterialTheme.colorScheme
    val container = when (item.action) {
        PreviewAction.INVALID -> scheme.errorContainer.copy(alpha = 0.4f)
        PreviewAction.UNCHANGED -> scheme.surfaceContainerLow
        else -> scheme.surfaceContainerLow
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionBadge(item.action)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.validation_row_number, item.rowNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (!item.externalKey.isNullOrBlank()) {
                    Text(
                        text = item.externalKey,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            when (item.action) {
                PreviewAction.INSERT -> if (!item.name.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.name, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                }
                PreviewAction.UPDATE -> item.changes.forEach { change ->
                    ChangeLine(change)
                }
                PreviewAction.INVALID -> item.errors.forEach { error ->
                    val field = stringResource(error.field.fieldLabelRes())
                    val message = stringResource(error.code.validationErrorRes())
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.validation_error_line, field, message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error
                    )
                }
                PreviewAction.UNCHANGED -> {}
            }
        }
    }
}

@Composable
private fun ChangeLine(change: FieldChange) {
    val scheme = MaterialTheme.colorScheme
    val field = stringResource(change.field.fieldLabelRes())
    Spacer(Modifier.height(2.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = field,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = change.from ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text("→", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            text = change.to ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurface
        )
    }
}

@Composable
private fun ActionBadge(action: PreviewAction) {
    val scheme = MaterialTheme.colorScheme
    val (label, container, content) = when (action) {
        PreviewAction.INSERT -> Triple(stringResource(R.string.preview_badge_insert), scheme.primaryContainer, scheme.onPrimaryContainer)
        PreviewAction.UPDATE -> Triple(stringResource(R.string.preview_badge_update), scheme.tertiaryContainer, scheme.onTertiaryContainer)
        PreviewAction.UNCHANGED -> Triple(stringResource(R.string.preview_badge_unchanged), scheme.surfaceVariant, scheme.onSurfaceVariant)
        PreviewAction.INVALID -> Triple(stringResource(R.string.preview_badge_invalid), scheme.errorContainer, scheme.onErrorContainer)
    }
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = content)
    }
}

/** Warning shown when all valid rows will be inserted as new products (no updates). */
@Composable
private fun AllNewWarning() {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.tertiaryContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = scheme.onTertiaryContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.preview_warn_all_new),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onTertiaryContainer
        )
    }
}

/**
 * Blocking confirmation shown before executing an import where nothing will be updated —
 * decided from the backend's own insert/update counts, not from which screen opened the flow.
 */
@Composable
private fun AllNewConfirmDialog(
    insertCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.preview_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.mapping_back))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.preview_confirm_all_new_title, insertCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
        },
        text = {
            Text(
                text = stringResource(R.string.preview_warn_all_new),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewScreenPreview() {
    BrandoraTheme {
        PreviewScreen(
            state = PreviewUiState(
                isLoading = false,
                summary = PreviewSummary(
                    totalRows = 5, validRows = 4, invalidRows = 1, skippedRows = 0,
                    willInsert = 2, willUpdate = 1, unchanged = 1
                ),
                totalCount = 5,
                nextOffset = null,
                items = listOf(
                    PreviewItem(2, PreviewAction.INSERT, externalKey = "SKU-001", name = "قميص"),
                    PreviewItem(3, PreviewAction.UPDATE, externalKey = "SKU-002", changes = listOf(
                        FieldChange("basePrice", "299.99", "249.99"),
                        FieldChange("stockQuantity", "12", "8")
                    )),
                    PreviewItem(4, PreviewAction.UNCHANGED, externalKey = "SKU-003"),
                    PreviewItem(5, PreviewAction.INVALID, errors = listOf(RowError("name", "REQUIRED")))
                )
            ),
            onEvent = {}
        )
    }
}
