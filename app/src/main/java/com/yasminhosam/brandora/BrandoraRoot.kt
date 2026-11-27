package com.yasminhosam.brandora

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import com.yasminhosam.brandora.app.AppNavHost
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.core.navigation.clearTo
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.SessionExpiredSignal
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import com.yasminhosam.brandora.feature.business.domain.usecase.BusinessSyncResult
import com.yasminhosam.brandora.feature.business.domain.usecase.ReconcileBusinessUseCase
import kotlinx.coroutines.flow.first

@Composable
fun BrandoraRoot(
    reconcileBusiness: ReconcileBusinessUseCase,
    sessionStore: SessionStore,
    sessionExpiredSignal: SessionExpiredSignal,
) {
    val nav = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by nav.currentBackStackEntryAsState()
    val sessionExpiredMessage = stringResource(R.string.session_expired)

    // Wait for the first real values before choosing the start screen: no session means
    // Login; with a session reconcileBusiness decides Home vs Business Create.
    var startDestination by remember { mutableStateOf<String?>(null) }
    var startupError by remember { mutableStateOf<ApiResult.Error?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryCount) {
        val session = sessionStore.session.first()
        if (session == null) {
            startDestination = Routes.LOGIN
        } else {
            startupError = null
            when (val sync = reconcileBusiness(session.user.id)) {
                BusinessSyncResult.HasBusiness -> startDestination = Routes.HOME
                BusinessSyncResult.NoBusiness -> startDestination = Routes.BUSINESS_CREATE
                is BusinessSyncResult.Error -> {
                    startupError = sync.error
                }
            }
        }
    }

    // Session expiry (refresh rejected or its response lost) lands on Login from anywhere,
    // with one snackbar. clearTo() clears the whole stack first (see its KDoc), so Login
    // ends up as the ONLY entry. Navigation runs before the (suspending) snackbar so the
    // redirect is immediate; the host survives it.
    LaunchedEffect(Unit) {
        sessionExpiredSignal.events.collect {
            nav.clearTo(Routes.LOGIN)
            snackbarHostState.showSnackbar(sessionExpiredMessage)
        }
    }

    // Products hides the bar on its nested screens (detail/inventory/editors) by writing this;
    // every other destination relies on isTopLevel() alone.
    val bottomBarVisible = rememberSaveable { mutableStateOf(true) }

    CompositionLocalProvider(LocalBottomBarVisible provides bottomBarVisible) {
        Scaffold(
            // Screens handle the status bar themselves; avoid applying it twice.
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = backStackEntry?.destination.isTopLevel() && bottomBarVisible.value,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                ) {
                    AppBottomBar(nav)
                }
            },
        ) { innerPadding ->
            val start = startDestination
            if (start == null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val error = startupError
                        if (error != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp),
                            ) {
                                Text(
                                    stringResource(error.type.messageRes()),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { retryCount++ }) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                }
            } else {
                AppNavHost(
                    nav = nav,
                    startDestination = start,
                    snackbarHostState = snackbarHostState,
                    // The bar's height already carries the navigation-bar inset, so consuming
                    // innerPadding keeps screens that add navigationBarsPadding() from doubling
                    // it — and stops consuming the moment the bar is hidden.
                    modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
                )
            }
        }
    }
}
