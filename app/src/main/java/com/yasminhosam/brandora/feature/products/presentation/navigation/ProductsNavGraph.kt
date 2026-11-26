package com.yasminhosam.brandora.feature.products.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.feature.products.presentation.ProductsRoute

/**
 * Nested products graph behind the bottom-bar tab. Browse is the only destination: detail, the
 * two editors, the variant screens and the inventory screen run inside [ProductsRoute] as one
 * compose state machine, so they deliberately are not separate destinations.
 */
fun NavGraphBuilder.productsGraph(onImport: () -> Unit) {
    navigation(startDestination = Routes.PRODUCTS_BROWSE, route = Routes.PRODUCTS) {
        composable(Routes.PRODUCTS_BROWSE) { ProductsRoute(onImport = onImport) }
    }
}
