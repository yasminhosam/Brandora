package com.yasminhosam.brandora.feature.products.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.core.ui.theme.extended
import com.yasminhosam.brandora.feature.products.domain.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle

/**
 * Movement timestamps are ISO-8601 from the API; show them as a medium date-time in the
 * device's locale so merchants read a clock time instead of a UTC string. Anything the
 * parser does not recognise is left exactly as the server sent it.
 */
@Composable
private fun MovementTime(iso: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(iso, locale) {
        try {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .format(Instant.parse(iso).atZone(ZoneId.systemDefault()))
        } catch (e: DateTimeParseException) {
            iso
        }
    }
}

/**
 * Variant inventory: exact quantity with its tracking state, the entry point to the absolute
 * quantity dialog (StockDialog at the route level) and the movement history that used to live
 * inline on the product detail screen.
 */
@Composable
internal fun VariantInventory(
    state: ProductsState,
    event: (ProductsEvent) -> Unit,
    onStock: (Variant) -> Unit,
) {
    val product = state.selected ?: return
    val variantId = state.historyVariant ?: return
    val variant = product.variants.firstOrNull { it.id == variantId } ?: return
    Text(product.name, style = MaterialTheme.typography.headlineSmall)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (variant.optionValues.isEmpty())
                stringResource(R.string.pm_default_variant)
            else variant.optionValues.entries.joinToString(" / ") { it.key + ": " + it.value },
            style = MaterialTheme.typography.titleMedium,
        )
        VariantStatusBadge(variant)
    }
    variant.sku?.let {
        Text(
            stringResource(R.string.pm_sku_value, it),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extended.textMuted,
        )
    }

    SectionHeader(R.string.pm_inventory)
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Exact stock lives here and on the detail cards — never on browse cards.
            Text(
                if (variant.trackInventory)
                    stringResource(R.string.pm_stock_value, variant.stockQuantity ?: 0)
                else stringResource(R.string.pm_untracked),
                style = MaterialTheme.typography.titleLarge,
                color = stockColor(variant),
            )
            Button(
                { onStock(variant) },
                enabled = !state.busy && variant.isActive && product.isActive,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(52.dp),
            ) {
                Text(stringResource(R.string.pm_adjust))
            }
        }
    }

    SectionHeader(R.string.pm_history)
    if (state.movements.isEmpty())
        Text(
            stringResource(R.string.pm_no_movements),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.extended.textMuted,
        )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.movements.forEach { m ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    MovementTime(m.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extended.textMuted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    (m.quantityBefore?.toString() ?: "—") +
                        " → " +
                        (m.quantityAfter?.toString() ?: "—"),
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        when {
                            m.quantityDelta < 0 -> MaterialTheme.extended.danger.content
                            m.quantityDelta > 0 -> MaterialTheme.extended.success.content
                            else -> MaterialTheme.extended.textMuted
                        },
                )
            }
        }
    }
    if (state.movementPage.hasMore)
        TextButton({ event(ProductsEvent.MoreHistory) }, enabled = !state.busy) {
            Text(stringResource(R.string.pm_more))
        }
}

@Preview(name = "Variant inventory", showBackground = true)
@Composable
private fun VariantInventoryPreview() {
    BrandoraTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            VariantInventory(
                state =
                    ProductsState(
                        businessId = "b",
                        selected =
                            Product(
                                "p1",
                                "b",
                                "Medjool Dates",
                                variants =
                                    listOf(
                                        Variant(
                                            "v1",
                                            "p1",
                                            "b",
                                            sku = "D-500",
                                            optionValues = mapOf("Size" to "500g"),
                                            trackInventory = true,
                                            stockQuantity = 12,
                                        ),
                                    ),
                            ),
                        historyVariant = "v1",
                        movements =
                            listOf(
                                StockMovement(
                                    id = "m1",
                                    type = "ADJUSTMENT",
                                    quantityBefore = 8,
                                    quantityAfter = 12,
                                    quantityDelta = 4,
                                    createdAt = "2026-11-08T10:00:00Z",
                                ),
                            ),
                    ),
                event = {},
                onStock = {},
            )
        }
    }
}
