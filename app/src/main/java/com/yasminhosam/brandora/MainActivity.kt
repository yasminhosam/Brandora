package com.yasminhosam.brandora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yasminhosam.brandora.feature.catalogimport.presentation.navigation.CatalogImportNavHost
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrandoraTheme {
                CatalogImportNavHost()
            }
        }
    }
}