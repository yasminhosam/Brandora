package com.yasminhosam.brandora.feature.catalogimport.presentation.navigation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.feature.catalogimport.presentation.mapping.MappingEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.mapping.MappingScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.mapping.MappingViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadEvent
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadViewModel

object CatalogImportDestinations {
    const val UPLOAD = "catalog_import/upload"

    /** TEMP: placeholder for the future Add Product screen; replaced when that screen lands. */
    const val ADD_PRODUCT = "catalog_import/add_product"

    const val MAPPING = "catalog_import/mapping/{importId}"

    fun mapping(importId: String) = "catalog_import/mapping/$importId"
}

@Composable
fun CatalogImportNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = CatalogImportDestinations.UPLOAD,
        modifier = modifier
    ) {
        composable(CatalogImportDestinations.UPLOAD) {
            val viewModel: UploadViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            val picker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    // The stream opens lazily at upload time, so the grant must persist.
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                        // Providers without persistable grants still read fine this session.
                    }
                    viewModel.onEvent(UploadEvent.FilePicked(uri.toString()))
                }
            }

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is UploadEffect.NavigateToMapping ->
                            navController.navigate(CatalogImportDestinations.mapping(effect.importId))
                        UploadEffect.NavigateToManualEntry ->
                            navController.navigate(CatalogImportDestinations.ADD_PRODUCT)
                    }
                }
            }

            UploadScreen(
                state = state,
                onEvent = viewModel::onEvent,
                // Unfiltered on purpose: picking invalid files is how local validation gets exercised.
                onBrowseClick = { picker.launch(arrayOf("*/*")) }
            )
        }

        composable(CatalogImportDestinations.ADD_PRODUCT) {
            AddProductPlaceholderScreen()
        }

        composable(
            route = CatalogImportDestinations.MAPPING,
            arguments = listOf(navArgument(ApiParams.PATH_IMPORT_ID) { type = NavType.StringType })
        ) {
            val viewModel: MappingViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        // TEMP: the validate route lands in the validation slice; until then the
                        // mapping step just returns so this commit compiles on its own.
                        is MappingEffect.NavigateToValidate -> navController.popBackStack()
                        MappingEffect.NavigateBack -> navController.popBackStack()
                    }
                }
            }

            MappingScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
}
