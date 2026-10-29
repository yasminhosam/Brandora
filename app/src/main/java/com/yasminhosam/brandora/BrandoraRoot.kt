package com.yasminhosam.brandora

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yasminhosam.brandora.app.AppNavHost
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.core.network.SessionExpiredSignal
import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import kotlinx.coroutines.flow.first

@Composable
fun BrandoraRoot(
    currentBusinessStore: CurrentBusinessStore,
    sessionStore: SessionStore,
    sessionExpiredSignal: SessionExpiredSignal
) {
    val nav = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by nav.currentBackStackEntryAsState()
    val sessionExpiredMessage = stringResource(R.string.session_expired)

    // Wait for the first real values before choosing the start screen: no session means
    // Login; with a session the existing business gate decides Home vs Business Create.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDestination = when {
            sessionStore.session.first() == null -> Routes.LOGIN
            currentBusinessStore.businessId.first() == null -> Routes.BUSINESS_CREATE
            else -> Routes.HOME
        }
    }

    // Session expiry (refresh rejected or its response lost) lands on Login from anywhere,
    // with one snackbar. The stack is fully cleared first, because popUpTo(start) silently
    // no-ops when the start destination isn't on the back stack (a real case: the process
    // started at Login, the user logged in, so Login itself is long gone). popBackStack()
    // only removes the top entry, so walk down to the bottom entry and let the navigate()'s
    // popUpTo remove that one too: Login ends up as the ONLY entry, and system back exits
    // the app instead of revealing a session-less screen behind it. Navigation runs before
    // the (suspending) snackbar so the redirect is immediate; the host survives it.
    LaunchedEffect(Unit) {
        sessionExpiredSignal.events.collect {
            while (nav.previousBackStackEntry != null) {
                if (!nav.popBackStack()) break
            }
            val bottomId = nav.currentBackStackEntry?.destination?.id
            nav.navigate(Routes.LOGIN) {
                if (bottomId != null) popUpTo(bottomId) { inclusive = true }
                launchSingleTop = true
            }
            snackbarHostState.showSnackbar(sessionExpiredMessage)
        }
    }

    Scaffold(
        // Screens handle the status bar themselves; avoid applying it twice.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (backStackEntry?.destination.isTopLevel()) AppBottomBar(nav)
        }
    ) { innerPadding ->
        val start = startDestination
        if (start == null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {}
        } else {
            AppNavHost(
                nav = nav,
                startDestination = start,
                snackbarHostState = snackbarHostState,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
