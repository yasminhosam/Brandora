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
import com.yasminhosam.brandora.core.navigation.clearTo
import com.yasminhosam.brandora.feature.auth.presentation.forgot.ForgotScreen
import com.yasminhosam.brandora.feature.auth.presentation.forgot.ForgotViewModel
import com.yasminhosam.brandora.feature.auth.presentation.login.LoginEffect
import com.yasminhosam.brandora.feature.auth.presentation.login.LoginScreen
import com.yasminhosam.brandora.feature.auth.presentation.login.LoginViewModel
import com.yasminhosam.brandora.feature.auth.presentation.reset.ResetEffect
import com.yasminhosam.brandora.feature.auth.presentation.reset.ResetEvent
import com.yasminhosam.brandora.feature.auth.presentation.reset.ResetScreen
import com.yasminhosam.brandora.feature.auth.presentation.reset.ResetTokenHolder
import com.yasminhosam.brandora.feature.auth.presentation.reset.ResetViewModel
import com.yasminhosam.brandora.feature.auth.presentation.register.RegisterEffect
import com.yasminhosam.brandora.feature.auth.presentation.register.RegisterScreen
import com.yasminhosam.brandora.feature.auth.presentation.register.RegisterViewModel
import com.yasminhosam.brandora.feature.business.presentation.BusinessEffect
import com.yasminhosam.brandora.feature.business.presentation.BusinessScreen
import com.yasminhosam.brandora.feature.business.presentation.BusinessViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.navigation.importGraph
import com.yasminhosam.brandora.feature.dashboard.presentation.DashboardPlaceholderScreen
import com.yasminhosam.brandora.feature.orders.presentation.HomeScreen
import com.yasminhosam.brandora.feature.orders.presentation.OrdersPlaceholderScreen
import com.yasminhosam.brandora.feature.products.presentation.ProductsPlaceholderScreen
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    nav: NavHostController,
    startDestination: String,
    snackbarHostState: SnackbarHostState,
    resetTokenHolder: ResetTokenHolder,
    modifier: Modifier = Modifier
) {
    // Scoped to AppNavHost (not the Result screen) so the snackbar survives the pop
    // back to whichever tab opened the import.
    val appScope = rememberCoroutineScope()
    val importDoneMessage = stringResource(R.string.import_success)
    val resetDoneMessage = stringResource(R.string.reset_done)

    NavHost(nav, startDestination = startDestination, modifier = modifier) {

        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is LoginEffect.LoginSucceeded -> {
                            // Business Create (when no business exists yet) keeps its
                            // existing post-create flow; Home is the normal landing.
                            nav.navigate(effect.nextRoute) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }
                }
            }

            LoginScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onRegisterClick = { nav.navigate(Routes.REGISTER) },
                onForgotPasswordClick = { nav.navigate(Routes.FORGOT_PASSWORD) }
            )
        }

        composable(Routes.REGISTER) {
            val viewModel: RegisterViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is RegisterEffect.RegisterSucceeded -> {
                            // Register always sits directly above Login: pop both so Back
                            // can't return to an auth form once the session exists.
                            nav.navigate(effect.nextRoute) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }
                }
            }

            RegisterScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onLoginClick = {
                    // Pop back to Login; the fallback only runs if it isn't underneath.
                    if (!nav.popBackStack()) {
                        nav.navigate(Routes.LOGIN) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            val viewModel: ForgotViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            ForgotScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onBackToLoginClick = {
                    // Pop back to Login; the fallback only runs if it isn't underneath.
                    if (!nav.popBackStack()) {
                        nav.navigate(Routes.LOGIN) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(Routes.RESET_PASSWORD) {
            val viewModel: ResetViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Hand the app-link token to the ViewModel and consume it from the holder.
            // Registered here (not in BrandoraRoot) so the token arrives on this screen;
            // a newer link while the screen is open simply replaces the held token.
            LaunchedEffect(Unit) {
                resetTokenHolder.token.filterNotNull().collect { token ->
                    resetTokenHolder.consume()
                    viewModel.onEvent(ResetEvent.TokenArrived(token))
                }
            }

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is ResetEffect.ResetSucceeded -> {
                            // The 204 revoked every session server-side and the repository
                            // cleared the local one: clearTo() removes the whole stack (a
                            // logged-in flow may sit under this screen), Login becomes the
                            // only entry, and one root-scoped snackbar confirms it.
                            nav.clearTo(Routes.LOGIN)
                            appScope.launch { snackbarHostState.showSnackbar(resetDoneMessage) }
                        }
                    }
                }
            }

            ResetScreen(state = state, onEvent = viewModel::onEvent)
        }

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

    // Registered AFTER NavHost so its graph is already installed before we navigate (a
    // reset link can arrive on the very first frames of a cold start). A link opens the
    // Reset screen on top of whatever is showing - including a logged-in session; the
    // Reset screen itself consumes the token, so this collector never clears it.
    LaunchedEffect(Unit) {
        resetTokenHolder.token.filterNotNull().collect {
            nav.navigate(Routes.RESET_PASSWORD) { launchSingleTop = true }
        }
    }
}

