package com.yasminhosam.brandora.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.feature.business.presentation.BusinessEffect
import com.yasminhosam.brandora.feature.business.presentation.BusinessScreen
import com.yasminhosam.brandora.feature.business.presentation.BusinessViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.navigation.importGraph
import com.yasminhosam.brandora.feature.dashboard.presentation.DashboardPlaceholderScreen
import com.yasminhosam.brandora.feature.orders.presentation.HomeScreen
import com.yasminhosam.brandora.feature.orders.presentation.OrdersPlaceholderScreen
import com.yasminhosam.brandora.feature.products.presentation.ProductsPlaceholderScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    nav: NavHostController,
    startDestination: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // Scoped to AppNavHost (not the Result screen) so the snackbar survives the pop
    // back to whichever tab opened the import.
    val appScope = rememberCoroutineScope()
    val importDoneMessage = stringResource(R.string.import_success)

    NavHost(nav, startDestination = startDestination, modifier = modifier) {

        composable(Routes.BUSINESS_CREATE) {
            val viewModel: BusinessViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is BusinessEffect.BusinessCreated -> {
                            // Land directly in the import flow (Upload is the graph's start).
                            // Home stays underneath so Back and the post-import pop to Home
                            // always have a root destination to return to.
                            nav.navigate(Routes.HOME) {
                                // The business is created once; remove this screen from the stack.
                                popUpTo(Routes.BUSINESS_CREATE) { inclusive = true }
                            }
                            nav.navigate(Routes.IMPORT_GRAPH)
                        }
                    }
                }
            }

            BusinessScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(Routes.HOME) {
            HomeScreen(onImportClicked = { nav.navigate(Routes.IMPORT_GRAPH) })
        }

        composable(Routes.DASHBOARD) { DashboardPlaceholderScreen(Modifier.fillMaxSize()) }
        composable(Routes.ORDERS) { OrdersPlaceholderScreen(Modifier.fillMaxSize()) }

        composable(Routes.PRODUCTS) {
            ProductsPlaceholderScreen(onImportClick = { nav.navigate(Routes.IMPORT_GRAPH) })
        }

        // The completion snackbar is scoped above, so it survives the pop to the opener tab.
        importGraph(
            nav = nav,
            onImportDone = {
                appScope.launch { snackbarHostState.showSnackbar(importDoneMessage) }
            }
        )
    }
}

