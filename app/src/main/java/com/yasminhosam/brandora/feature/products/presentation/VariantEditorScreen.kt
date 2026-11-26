@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.yasminhosam.brandora.feature.products.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.products.domain.ProductRules
import com.yasminhosam.brandora.feature.products.domain.model.*

/**
 * Create/edit variant: grouped sections (Details / Options / Inventory), a sticky Save+Cancel bar
 * that stays above the keyboard, and inline client-rule feedback after the first save attempt. The
 * create path sends sku/optionValues/unitPrice only (B8) — quantity is set from the inventory screen.
 */
@Composable
internal fun VariantEditor(
    product: Product,
    initial: Variant?,
    state: ProductsState,
    onCancel: () -> Unit,
    event: (ProductsEvent) -> Unit,
    onSave: (String?, VariantDraft) -> Unit,
) {
    val busy = state.busy
    var sku by rememberSaveable(initial?.id) { mutableStateOf(initial?.sku.orEmpty()) }
    var price by rememberSaveable(initial?.id) { mutableStateOf(initial?.unitPrice.orEmpty()) }
    var values by
        rememberSaveable(initial?.id) { mutableStateOf(initial?.optionValues ?: emptyMap<String, String>()) }
    var active by rememberSaveable(initial?.id) { mutableStateOf(initial?.isActive ?: true) }
    var saveAttempted by rememberSaveable { mutableStateOf(false) }
    // Client rules surface inline after the first save attempt; the gate below is unchanged.
    val skuError = saveAttempted && sku.trim().codePointCount(0, sku.trim().length) > 200
    val priceError = saveAttempted && !ProductRules.validMoney(price.trim())
    val selectionError = saveAttempted && !ProductRules.selectionMatches(product.options, values)
    // Nothing is discarded without asking: compare the draft against the saved variant.
    val dirty =
        sku != initial?.sku.orEmpty() ||
            price != initial?.unitPrice.orEmpty() ||
            values != (initial?.optionValues ?: emptyMap<String, String>()) ||
            active != (initial?.isActive ?: true)
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    // Composed after the route's own handler, so while the editor holds changes this one wins.
    BackHandler(enabled = dirty && !busy) { confirmDiscard = true }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(
                    if (initial == null) R.string.pm_add_variant else R.string.pm_edit_variant
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
            ProductError(state) { event(ProductsEvent.Retry) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            SectionHeader(R.string.pm_details)
            Field(
                sku,
                { sku = it },
                R.string.pm_sku,
                enabled = !busy,
                error = if (skuError) R.string.pm_sku_error else null,
            )
            Field(
                price,
                { price = it },
                R.string.pm_unit_price,
                enabled = !busy,
                keyboard = KeyboardType.Decimal,
                error = if (priceError) R.string.pm_price_error else null,
            )
            Text(
                stringResource(R.string.pm_price_inheritance),
                style = MaterialTheme.typography.bodySmall,
            )

            if (product.options.isNotEmpty()) {
                SectionHeader(R.string.pm_options)
                product.options.forEach { (name, allowed) ->
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        allowed.forEach { value ->
                            FilterChip(
                                values[name] == value,
                                { values = values + (name to value) },
                                label = { Text(value) },
                                enabled = !busy,
                            )
                        }
                    }
                }
                if (selectionError)
                    Text(
                        stringResource(R.string.pm_variant_invalid),
                        color = MaterialTheme.colorScheme.error,
                    )
            }

            SectionHeader(R.string.pm_inventory)
            if (initial != null)
                Row {
                    Checkbox(active, { active = it }, enabled = !busy)
                    Text(stringResource(R.string.pm_active))
                }
            // B8: creation carries no quantity, so point at the place where it is set.
            if (initial == null)
                Text(
                    stringResource(R.string.pm_set_stock_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                {
                    saveAttempted = true
                    // Identical gate to before: compute every rule, dispatch only when all pass.
                    val invalidNow =
                        !ProductRules.selectionMatches(product.options, values) ||
                            !ProductRules.validMoney(price.trim()) ||
                            sku.trim().codePointCount(0, sku.trim().length) > 200
                    if (!invalidNow)
                        onSave(
                            initial?.id,
                            VariantDraft(
                                sku = sku.trim().takeIf { it.isNotEmpty() },
                                unitPrice = price.trim().takeIf { it.isNotEmpty() },
                                optionValues = values,
                                isActive = if (initial != null) active else null,
                            ),
                        )
                },
                enabled = !busy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                if (busy)
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                else Text(stringResource(R.string.pm_save))
            }
            TextButton(
                { if (dirty) confirmDiscard = true else onCancel() },
                enabled = !busy,
            ) {
                Text(stringResource(R.string.pm_cancel))
            }
        }
    }
    if (confirmDiscard)
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.pm_confirm_title)) },
            text = { Text(stringResource(R.string.pm_confirm_discard)) },
            confirmButton = {
                TextButton(
                    {
                        confirmDiscard = false
                        onCancel()
                    }
                ) {
                    Text(stringResource(R.string.pm_discard))
                }
            },
            dismissButton = {
                TextButton({ confirmDiscard = false }) {
                    Text(stringResource(R.string.pm_cancel))
                }
            },
        )
}

@Preview(name = "Variant editor / create", showBackground = true)
@Composable
private fun VariantEditorCreatePreview() {
    BrandoraTheme {
        VariantEditor(
            product =
                Product(
                    "p1",
                    "b",
                    "T-Shirt",
                    options = mapOf("Size" to listOf("S", "M", "L")),
                ),
            initial = null,
            state = ProductsState(businessId = "b"),
            onCancel = {},
            event = {},
            onSave = { _, _ -> },
        )
    }
}
