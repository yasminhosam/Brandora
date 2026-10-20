package com.yasminhosam.brandora.feature.catalogimport.presentation.mapping

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.core.ui.theme.extended
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingChoice
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingInvalidReason
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.StandardTarget

@Composable
fun MappingScreen(
    state: MappingUiState,
    onEvent: (MappingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val warning = MaterialTheme.extended.warning
    val showSkuWarning = !state.isSkuMapped

    Column(
        // Edge-to-edge: keep content clear of the status and gesture bars.
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Simple toolbar — no step indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.primary, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = { onEvent(MappingEvent.BackClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.mapping_back),
                    tint = scheme.onPrimary
                )
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    text = stringResource(R.string.mapping_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary
                )
                Text(
                    text = stringResource(R.string.mapping_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            when {
                state.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                state.loadFailure != null -> {
                    val failure = state.loadFailure
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = stringResource(failure.messageRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onBackground
                            )
                            Spacer(Modifier.height(16.dp))
                            if (failure.retryable) {
                                Button(onClick = { onEvent(MappingEvent.RetryLoad) }) {
                                    Text(text = stringResource(R.string.action_retry))
                                }
                            } else {
                                OutlinedButton(onClick = { onEvent(MappingEvent.BackClicked) }) {
                                    Text(text = stringResource(R.string.mapping_back))
                                }
                            }
                        }
                    }
                }

                state.columns.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.mapping_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant
                        )
                    }

                else -> {
                    val claimed = state.columns.mapNotNull {
                        (it.choice as? MappingChoice.Standard)?.target
                    }.toSet()
                    val assigned = StandardTarget.entries.filter { it in claimed }
                    if (assigned.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            items(assigned, key = { it }) { target ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(scheme.primaryContainer, CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = scheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = targetLabel(target, markRequired = false),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = scheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (!state.isNameMapped) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(scheme.errorContainer, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.mapping_error_name_required),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (showSkuWarning) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(warning.container, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.mapping_warn_sku),
                                style = MaterialTheme.typography.bodyMedium,
                                color = warning.content
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(
                            state.columns,
                            key = { _, column -> column.sourceColumnIndex }
                        ) { index, column ->
                            MappingCard(
                                column = column,
                                displayHeader = column.sourceColumn.ifBlank {
                                    stringResource(
                                        R.string.mapping_column_number,
                                        column.sourceColumnIndex
                                    )
                                },
                                claimedTargets = claimed,
                                keyErrorRes = state.keyError(index)?.let { reasonRes(it) },
                                onChoice = { onEvent(MappingEvent.ChoiceChanged(index, it)) },
                                onKey = { onEvent(MappingEvent.CustomKeyChanged(index, it)) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(12.dp))
                        state.confirmFailureRes?.let { res ->
                            Text(
                                text = stringResource(res),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { onEvent(MappingEvent.ConfirmClicked) },
                            enabled = state.canConfirm,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (state.isConfirming) {
                                CircularProgressIndicator(
                                    Modifier.size(20.dp),
                                    color = scheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.mapping_confirm),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MappingCard(
    column: MappedColumn,
    displayHeader: String,
    claimedTargets: Set<StandardTarget>,
    keyErrorRes: Int?,
    onChoice: (MappingChoice) -> Unit,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            if ((column.choice as? MappingChoice.Standard)?.target == StandardTarget.NAME) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "* " + stringResource(R.string.mapping_required),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.error
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = displayHeader,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    column.samples.take(3).forEach { sample ->
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(scheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(18.dp)
                )
                ChoiceDropdown(
                    choice = column.choice,
                    claimedTargets = claimedTargets,
                    onChoice = onChoice,
                    modifier = Modifier.width(150.dp)
                )
            }
            val customKey = (column.choice as? MappingChoice.CustomAttribute)?.key
            if (customKey != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customKey,
                    onValueChange = onKey,
                    singleLine = true,
                    isError = keyErrorRes != null,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text(text = stringResource(keyErrorRes ?: R.string.mapping_key_hint))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceDropdown(
    choice: MappingChoice,
    claimedTargets: Set<StandardTarget>,
    onChoice: (MappingChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val currentTarget = (choice as? MappingChoice.Standard)?.target
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = choiceLabel(choice),
            onValueChange = {},
            readOnly = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor = scheme.primary,
                unfocusedBorderColor = scheme.primary,
                focusedContainerColor = scheme.primaryContainer.copy(alpha = 0.3f),
                unfocusedContainerColor = scheme.primaryContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            StandardTarget.entries.forEach { target ->
                DropdownMenuItem(
                    text = { Text(text = targetLabel(target)) },
                    onClick = {
                        onChoice(MappingChoice.Standard(target))
                        expanded = false
                    },
                    enabled = target == currentTarget || target !in claimedTargets
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.mapping_choice_custom)) },
                onClick = {
                    onChoice(MappingChoice.CustomAttribute(""))
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.mapping_choice_ignore)) },
                onClick = {
                    onChoice(MappingChoice.Ignore)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun choiceLabel(choice: MappingChoice): String = when (choice) {
    is MappingChoice.Standard -> targetLabel(choice.target)
    is MappingChoice.CustomAttribute -> stringResource(R.string.mapping_choice_custom)
    MappingChoice.Ignore -> stringResource(R.string.mapping_choice_ignore)
}

@Composable
private fun targetLabel(target: StandardTarget, markRequired: Boolean = true): String {
    val label = stringResource(
        when (target) {
            StandardTarget.NAME -> R.string.mapping_target_name
            StandardTarget.EXTERNAL_KEY -> R.string.mapping_target_external_key
            StandardTarget.DESCRIPTION -> R.string.mapping_target_description
            StandardTarget.CATEGORY -> R.string.mapping_target_category
            StandardTarget.BASE_PRICE -> R.string.mapping_target_base_price
            StandardTarget.STOCK_QUANTITY -> R.string.mapping_target_stock_quantity
        }
    )
    return if (target == StandardTarget.NAME && markRequired) "$label *" else label
}

private fun reasonRes(reason: MappingInvalidReason): Int = when (reason) {
    is MappingInvalidReason.BadCustomKey -> R.string.mapping_error_key_format
    is MappingInvalidReason.CustomKeyUsedTwice -> R.string.mapping_error_key_duplicate
    else -> R.string.mapping_error_key
}

private fun previewColumn(
    name: String,
    index: Int,
    choice: MappingChoice
) = MappedColumn(name, index, listOf("Shirt", "Hat"), choice)

@Preview(name = "Filled", showBackground = true)
@Composable
private fun MappingFilledPreview() {
    BrandoraTheme {
        MappingScreen(
            state = MappingUiState(
                isLoading = false,
                columns = listOf(
                    previewColumn("Item Name", 1, MappingChoice.Standard(StandardTarget.NAME)),
                    previewColumn("Fabric", 2, MappingChoice.CustomAttribute("fabric")),
                    previewColumn("", 3, MappingChoice.Ignore)
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun MappingErrorPreview() {
    BrandoraTheme {
        MappingScreen(
            state = MappingUiState(
                isLoading = false,
                columns = listOf(
                    previewColumn("Item Name", 1, MappingChoice.Ignore),
                    previewColumn("Fabric", 2, MappingChoice.CustomAttribute("Fabric"))
                ),
                validation = MappingValidationResult.Invalid(
                    listOf(
                        MappingInvalidReason.NameMissing,
                        MappingInvalidReason.BadCustomKey(1)
                    )
                )
            ),
            onEvent = {}
        )
    }
}
