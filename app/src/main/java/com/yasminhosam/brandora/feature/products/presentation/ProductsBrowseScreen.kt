@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.yasminhosam.brandora.feature.products.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.core.ui.theme.extended
import com.yasminhosam.brandora.feature.products.domain.CategoryFilter
import com.yasminhosam.brandora.feature.products.domain.model.*

/** Browse list: category chip row, empty states, product cards, load more. */
@Composable
internal fun ProductsBrowse(
    state: ProductsState,
    event: (ProductsEvent) -> Unit,
    onManage: () -> Unit,
) {
    if (state.businessId != null) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                state.filter == CategoryFilter.All,
                {
                    if (state.filter != CategoryFilter.All) event(ProductsEvent.Filter(null))
                },
                label = { Text(stringResource(R.string.pm_filter_all)) },
                enabled = !state.busy,
            )
            state.categories.forEach { c ->
                FilterChip(
                    state.filter == CategoryFilter.Category(c.id),
                    {
                        if (state.filter != CategoryFilter.Category(c.id))
                            event(ProductsEvent.Filter(c.id))
                    },
                    label = { Text(c.name) },
                    enabled = !state.busy,
                )
            }
            FilterChip(
                selected = false,
                onClick = onManage,
                label = { },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.pm_manage_categories),
                    )
                },
                enabled = !state.busy,
            )
        }
    }
    if (state.items.isEmpty()) {
        // Initial load / refresh: skeleton cards instead of a bare progress line.
        AnimatedVisibility(visible = state.busy, enter = fadeIn(), exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { ProductCardSkeleton() }
            }
        }
        AnimatedVisibility(
            visible = !state.busy,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            EmptyState(
                if (state.filter == CategoryFilter.All) R.string.pm_empty
                else R.string.pm_empty_category,
                icon = Icons.Filled.ShoppingCart,
            )
        }
    }
    state.items.forEach { p ->
        Card(
            onClick = { event(ProductsEvent.Open(p.id)) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Fixed slot: cards keep the same text alignment whether or not the product
                // has an image, instead of jumping left when the main photo is missing.
                Box(
                    modifier =
                        Modifier.size(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    (p.images.firstOrNull { it.isMain } ?: p.images.firstOrNull())?.let {
                        AsyncImage(
                            it.secureUrl,
                            p.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    // Server-computed fields only: base price and sale eligibility, never a sum
                    // of the embedded first variant page.
                    Text(
                        p.basePrice ?: stringResource(R.string.pm_unpriced),
                        style =
                            if (p.basePrice != null) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.bodyMedium,
                        color =
                            if (p.basePrice != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.extended.textMuted,
                    )
                    ProductStatusBadge(p)
                }
            }
        }
    }
    if (state.pagination.hasMore)
        TextButton({ event(ProductsEvent.More) }, enabled = !state.busy) {
            Text(stringResource(R.string.pm_more))
        }
    // Keeps "Load more" clear of the re-import FAB.
    if (state.items.isNotEmpty()) Spacer(Modifier.height(72.dp))
}

@Preview(name = "Browse / empty", showBackground = true)
@Composable
private fun ProductsBrowseEmptyPreview() {
    BrandoraTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ProductsBrowse(
                state =
                    ProductsState(
                        businessId = "b",
                        categories = listOf(Category("c1", "Dates"), Category("c2", "Nuts")),
                        categoriesLoaded = true,
                    ),
                event = {},
                onManage = {},
            )
        }
    }
}

@Preview(name = "Browse / filtered list", showBackground = true)
@Composable
private fun ProductsBrowseFilteredPreview() {
    BrandoraTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ProductsBrowse(
                state =
                    ProductsState(
                        businessId = "b",
                        categories = listOf(Category("c1", "Dates"), Category("c2", "Nuts")),
                        categoriesLoaded = true,
                        filter = CategoryFilter.Category("c1"),
                        items =
                            listOf(
                                Product(
                                    "p1",
                                    "b",
                                    "Medjool Dates",
                                    categoryId = "c1",
                                    basePrice = "25.00",
                                    isSellable = true,
                                ),
                                Product(
                                    "p2",
                                    "b",
                                    "Deglet Noor",
                                    categoryId = "c1",
                                    isActive = false,
                                ),
                                Product(
                                    "p3",
                                    "b",
                                    "Sample Pack",
                                    categoryId = "c1",
                                    isSellable = false,
                                ),
                            ),
                        pagination = PageInfo(hasMore = true, nextOffset = 20),
                    ),
                event = {},
                onManage = {},
            )
        }
    }
}

@Preview(name = "Browse / category empty", showBackground = true)
@Composable
private fun ProductsBrowseCategoryEmptyPreview() {
    BrandoraTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ProductsBrowse(
                state =
                    ProductsState(
                        businessId = "b",
                        categories = listOf(Category("c1", "Dates")),
                        categoriesLoaded = true,
                        filter = CategoryFilter.Category("c1"),
                    ),
                event = {},
                onManage = {},
            )
        }
    }
}

@Preview(name = "Browse / loading", showBackground = true)
@Composable
private fun ProductsBrowseLoadingPreview() {
    BrandoraTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ProductsBrowse(
                state = ProductsState(businessId = "b", busy = true),
                event = {},
                onManage = {},
            )
        }
    }
}
