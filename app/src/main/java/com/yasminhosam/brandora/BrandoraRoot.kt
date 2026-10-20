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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yasminhosam.brandora.app.AppNavHost
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import kotlinx.coroutines.flow.first

@Composable
fun BrandoraRoot(currentBusinessStore: CurrentBusinessStore) {
    val nav = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by nav.currentBackStackEntryAsState()

    // Wait for the first real DataStore value before choosing the start screen.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDestination =
            if (currentBusinessStore.businessId.first() == null) Routes.BUSINESS_CREATE
            else Routes.HOME
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