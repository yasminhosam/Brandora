package com.yasminhosam.brandora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import com.yasminhosam.brandora.feature.catalogimport.presentation.navigation.CatalogImportDestinations
import com.yasminhosam.brandora.feature.catalogimport.presentation.navigation.CatalogImportNavHost
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var currentBusinessStore: CurrentBusinessStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrandoraTheme {
                // The persisted business id is the source of truth - no GET at startup.
                val businessId by currentBusinessStore.businessId.collectAsStateWithLifecycle(
                    lifecycle = LocalLifecycleOwner.current.lifecycle,
                    initialValue = null
                )
                var ready by remember { mutableStateOf(false) }
                LaunchedEffect(businessId) { ready = true }

                if (!ready) {
                    // Blank themed surface while the first DataStore emission lands.
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) { }
                } else {
                    CatalogImportNavHost(
                        startDestination = if (businessId == null) {
                            CatalogImportDestinations.BUSINESS_CREATE
                        } else {
                            CatalogImportDestinations.UPLOAD
                        }
                    )
                }
            }
        }
    }
}
