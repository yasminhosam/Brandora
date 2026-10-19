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
import com.yasminhosam.brandora.feature.business.presentation.BusinessEffect
import com.yasminhosam.brandora.feature.business.presentation.BusinessScreen
import com.yasminhosam.brandora.feature.business.presentation.BusinessViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.mapping.MappingEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.mapping.MappingScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.mapping.MappingViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.preview.PreviewEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.preview.PreviewScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.preview.PreviewViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.result.ResultEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.result.ResultScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.result.ResultViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadEvent
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.validation.ValidationEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.validation.ValidationScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.validation.ValidationViewModel

object CatalogImportDestinations {
    const val UPLOAD = "catalog_import/upload"

    const val BUSINESS_CREATE = "catalog_import/business/create"

    /** TEMP: placeholder for the future Add Product screen; replaced when that screen lands. */
    const val ADD_PRODUCT = "catalog_import/add_product"

    const val MAPPING = "catalog_import/mapping/{importId}"
    const val VALIDATE = "catalog_import/validate/{importId}"
    const val PREVIEW = "catalog_import/preview/{importId}"
    const val RESULT = "catalog_import/result/{importId}"

    fun mapping(importId: String) = "catalog_import/mapping/$importId"
    fun validate(importId: String) = "catalog_import/validate/$importId"
    fun preview(importId: String) = "catalog_import/preview/$importId"
    fun result(importId: String) = "catalog_import/result/$importId"
}

@Composable
fun CatalogImportNavHost(
    modifier: Modifier = Modifier,
    startDestination: String = CatalogImportDestinations.UPLOAD
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(CatalogImportDestinations.BUSINESS_CREATE) {
            val viewModel: BusinessViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is BusinessEffect.BusinessCreated ->
                            navController.navigate(CatalogImportDestinations.UPLOAD) {
                                // The business is created once; drop the create screen from the back stack.
                                popUpTo(CatalogImportDestinations.BUSINESS_CREATE) { inclusive = true }
                            }
                    }
                }
            }

            BusinessScreen(
                state = state,
                onEvent = viewModel::onEvent
            )
        }

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
                        is MappingEffect.NavigateToValidate ->
                            navController.navigate(CatalogImportDestinations.validate(effect.importId)) {
                                // Validation starts on entry; mapping is no longer reachable.
                                popUpTo(CatalogImportDestinations.MAPPING) { inclusive = true }
                            }
                        MappingEffect.NavigateBack -> navController.popBackStack()
                    }
                }
            }

            MappingScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(
            route = CatalogImportDestinations.VALIDATE,
            arguments = listOf(navArgument(ApiParams.PATH_IMPORT_ID) { type = NavType.StringType })
        ) { entry ->
            val importId = entry.arguments?.getString(ApiParams.PATH_IMPORT_ID).orEmpty()
            val viewModel: ValidationViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        ValidationEffect.NavigateToPreview ->
                            navController.navigate(CatalogImportDestinations.preview(importId))
                        ValidationEffect.NavigateToUpload ->
                            navController.popBackStack(CatalogImportDestinations.UPLOAD, inclusive = false)
                    }
                }
            }

            ValidationScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(
            route = CatalogImportDestinations.PREVIEW,
            arguments = listOf(navArgument(ApiParams.PATH_IMPORT_ID) { type = NavType.StringType })
        ) { entry ->
            val importId = entry.arguments?.getString(ApiParams.PATH_IMPORT_ID).orEmpty()
            val viewModel: PreviewViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is PreviewEffect.NavigateToExecute ->
                            navController.navigate(CatalogImportDestinations.result(effect.importId)) {
                                popUpTo(CatalogImportDestinations.PREVIEW) { inclusive = true }
                            }
                        PreviewEffect.NavigateBack -> navController.popBackStack()
                    }
                }
            }

            PreviewScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(
            route = CatalogImportDestinations.RESULT,
            arguments = listOf(navArgument(ApiParams.PATH_IMPORT_ID) { type = NavType.StringType })
        ) { entry ->
            val viewModel: ResultViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        ResultEffect.NavigateToUpload ->
                            navController.popBackStack(CatalogImportDestinations.UPLOAD, inclusive = false)
                    }
                }
            }

            ResultScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
}
