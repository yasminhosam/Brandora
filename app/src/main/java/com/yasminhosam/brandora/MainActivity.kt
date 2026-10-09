package com.yasminhosam.brandora

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.core.ui.theme.Surface
import com.yasminhosam.brandora.core.ui.theme.extended
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.AnalyzeExcelFileUseCase
import com.yasminhosam.brandora.feature.catalogimport.presentation.navigation.CatalogImportNavHost
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

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