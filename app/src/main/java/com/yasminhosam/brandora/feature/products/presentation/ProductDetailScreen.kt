@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.yasminhosam.brandora.feature.products.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.core.ui.theme.StatusStyle
import com.yasminhosam.brandora.core.ui.theme.extended
import com.yasminhosam.brandora.feature.products.domain.model.*

/**
 * Product detail: a header with identity, sale state and price, then titled Details, Images
 * and Variants sections (inventory lives on its own screen).
 */
@Composable
internal fun ProductDetail(
    state: ProductsState,
    event: (ProductsEvent) -> Unit,
    onEdit: () -> Unit,
    onVariant: (Variant?) -> Unit,
    onConfirm: (Int, () -> Unit) -> Unit,
    onUpload: () -> Unit,
) {
    val product = state.selected ?: return
    var selectedImage by remember(product.id) { mutableIntStateOf(0) }
    val imageIndex = selectedImage.coerceIn(0, (product.images.size - 1).coerceAtLeast(0))
    val selected = product.images.getOrNull(imageIndex)

    // Header: identity, sale state and the price a merchant scans first.
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(product.name, style = MaterialTheme.typography.headlineSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProductStatusBadge(product)
                Text(
                    stringResource(
                        R.string.pm_base_value,
                        product.basePrice ?: stringResource(R.string.pm_unpriced),
                    ),
                    style =
                        if (product.basePrice != null) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                    color =
                        if (product.basePrice != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.extended.textMuted,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onEdit, enabled = !state.busy) {
                    Text(stringResource(R.string.pm_edit))
                }
                // B6: delete is gone — the lifecycle action is deactivate/reactivate behind a confirm.
                TextButton(
                    {
                        onConfirm(
                            if (product.isActive) R.string.pm_confirm_deactivate
                            else R.string.pm_confirm_reactivate,
                        ) { event(ProductsEvent.Active(!product.isActive)) }
                    },
                    enabled = !state.busy,
                ) {
                    Text(
                        stringResource(
                            if (product.isActive) R.string.pm_deactivate else R.string.pm_activate
                        ),
                        color =
                            if (product.isActive) MaterialTheme.colorScheme.error
                            else Color.Unspecified,
                    )
                }
            }
        }
    }

    if (product.description != null || product.externalKey != null) {
        SectionHeader(R.string.pm_details)
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                product.description?.let { Text(it) }
                product.externalKey?.let {
                    Text(
                        stringResource(R.string.pm_external_value, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extended.textMuted,
                    )
                }
            }
        }
    }

    SectionHeader(R.string.pm_images)
    Text(
        stringResource(R.string.pm_images_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.extended.textMuted,
    )
    if (product.images.isEmpty())
        Text(
            stringResource(R.string.pm_no_images),
            color = MaterialTheme.extended.textMuted,
        )
    if (selected != null) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(product.images) { index, image ->
                Box {
                    AsyncImage(
                        image.secureUrl,
                        product.name,
                        Modifier.size(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (index == imageIndex)
                                    Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp),
                                    )
                                else Modifier
                            )
                            .clickable { selectedImage = index },
                    )
                    if (image.isMain)
                        StatusBadge(
                            stringResource(R.string.pm_main),
                            StatusStyle(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer,
                            ),
                            Modifier.align(Alignment.TopStart).padding(6.dp),
                        )
                }
            }
        }
        // Every image action stays available, moved onto the selected thumbnail.
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!selected.isMain)
                IconButton({ event(ProductsEvent.Main(selected.id)) }, enabled = !state.busy) {
                    Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.pm_make_main))
                }
            IconButton(
                { event(ProductsEvent.Move(imageIndex, imageIndex - 1)) },
                enabled = !state.busy && imageIndex > 0,
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.pm_up))
            }
            IconButton(
                { event(ProductsEvent.Move(imageIndex, imageIndex + 1)) },
                enabled = !state.busy && imageIndex < product.images.lastIndex,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.pm_down),
                )
            }
            IconButton(
                {
                    onConfirm(R.string.pm_confirm_delete) {
                        event(ProductsEvent.DeleteImage(selected.id))
                    }
                },
                enabled = !state.busy,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.pm_delete))
            }
        }
    }
    state.pendingImageIds.forEach { id ->
        TextButton({ event(ProductsEvent.DeleteImage(id)) }, enabled = !state.busy) {
            Text(stringResource(R.string.pm_retry_cleanup))
        }
    }
    Button(
        { onUpload() },
        enabled = !state.busy && product.images.size < 5,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(52.dp),
    ) {
        Text(stringResource(R.string.pm_add_images, 5 - product.images.size))
    }

    SectionHeader(R.string.pm_variants)
    product.options.forEach { (name, values) ->
        Text(
            name + ": " + values.joinToString(" / "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.extended.textMuted,
        )
    }
    if (product.options.isNotEmpty() || product.variants.isEmpty())
        Button(
            { onVariant(null) },
            enabled = !state.busy && product.isActive,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(52.dp),
        ) {
            Text(stringResource(R.string.pm_add_variant))
        }
    product.variants.forEach { v ->
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (v.optionValues.isEmpty())
                            stringResource(R.string.pm_default_variant)
                        else
                            v.optionValues.entries.joinToString(" / ") { it.key + ": " + it.value },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    VariantStatusBadge(v)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    v.sku?.let {
                        Text(
                            stringResource(R.string.pm_sku_value, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extended.textMuted,
                        )
                    }
                    Text(
                        stringResource(
                            R.string.pm_effective_price,
                            v.effectivePrice ?: stringResource(R.string.pm_unpriced),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color =
                            if (v.effectivePrice != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.extended.textMuted,
                    )
                    Text(
                        if (v.trackInventory)
                            stringResource(R.string.pm_stock_value, v.stockQuantity ?: 0)
                        else stringResource(R.string.pm_untracked),
                        color = stockColor(v),
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton({ onVariant(v) }, enabled = !state.busy) {
                        Text(stringResource(R.string.pm_edit))
                    }
                    // Quantity and movement history live on the variant inventory screen.
                    TextButton(
                        { event(ProductsEvent.History(v.id)) },
                        enabled = !state.busy,
                    ) {
                        Text(stringResource(R.string.pm_inventory))
                    }
                    // B6 for variants: a state-only toggle; reactivation needs an active product.
                    TextButton(
                        { event(ProductsEvent.VariantActive(v.id, !v.isActive)) },
                        enabled = !state.busy && (v.isActive || product.isActive),
                    ) {
                        Text(
                            stringResource(
                                if (v.isActive) R.string.pm_deactivate else R.string.pm_activate
                            ),
                            color =
                                if (v.isActive) MaterialTheme.colorScheme.error
                                else Color.Unspecified,
                        )
                    }
                }
            }
        }
    }
    if (product.variantsPagination.hasMore)
        TextButton({ event(ProductsEvent.MoreVariants) }, enabled = !state.busy) {
            Text(stringResource(R.string.pm_more_variants))
        }
}

@Preview(name = "Product detail", showBackground = true)
@Composable
private fun ProductDetailPreview() {
    BrandoraTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ProductDetail(
                state =
                    ProductsState(
                        businessId = "b",
                        selected =
                            Product(
                                "p1",
                                "b",
                                "Medjool Dates",
                                categoryId = "c1",
                                basePrice = "25.00",
                                isSellable = true,
                                options = mapOf("Size" to listOf("500g", "1kg")),
                                variants =
                                    listOf(
                                        Variant(
                                            "v1",
                                            "p1",
                                            "b",
                                            sku = "D-500",
                                            unitPrice = "25.00",
                                            optionValues = mapOf("Size" to "500g"),
                                            trackInventory = true,
                                            stockQuantity = 12,
                                        ),
                                        Variant(
                                            "v2",
                                            "p1",
                                            "b",
                                            optionValues = mapOf("Size" to "1kg"),
                                            isActive = false,
                                        ),
                                    ),
                                variantsPagination =
                                    PageInfo(totalCount = 21, hasMore = true, nextOffset = 20),
                            ),
                    ),
                event = {},
                onEdit = {},
                onVariant = {},
                onConfirm = { _, _ -> },
                onUpload = {},
            )
        }
    }
}
