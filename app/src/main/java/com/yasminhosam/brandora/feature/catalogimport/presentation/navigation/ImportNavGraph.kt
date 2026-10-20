package com.yasminhosam.brandora.feature.catalogimport.presentation.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.feature.catalogimport.presentation.mapping.MappingEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.mapping.MappingScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.mapping.MappingViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.preview.PreviewEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.preview.PreviewScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.preview.PreviewViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.result.ResultEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.result.ResultEvent
import com.yasminhosam.brandora.feature.catalogimport.presentation.result.ResultScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.result.ResultViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadEvent
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.upload.UploadViewModel
import com.yasminhosam.brandora.feature.catalogimport.presentation.validation.ValidationEffect
import com.yasminhosam.brandora.feature.catalogimport.presentation.validation.ValidationScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.validation.ValidationViewModel

private val importIdArg = navArgument(ApiParams.PATH_IMPORT_ID) { type = NavType.StringType }

/**
 * Nested import graph: Upload → Mapping → Validate → Preview → Result.
 * [onImportDone] fires when the import flow finishes successfully and is popping back to
 * whichever tab opened it (used for the completion snackbar).
 */
fun NavGraphBuilder.importGraph(nav: NavHostController, onImportDone: () -> Unit = {}) {
    navigation(startDestination = Routes.UPLOAD, route = Routes.IMPORT_GRAPH) {

        composable(Routes.UPLOAD) {
            val viewModel: UploadViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            val picker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
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
                            nav.navigate(Routes.mapping(effect.importId))
                        UploadEffect.NavigateToManualEntry ->
                            nav.navigate(Routes.ADD_PRODUCT)
                    }
                }
            }

            UploadScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onBrowseClick = { picker.launch(arrayOf("*/*")) }
            )
        }

        composable(Routes.ADD_PRODUCT) { AddProductPlaceholderScreen() }

        composable(Routes.MAPPING, arguments = listOf(importIdArg)) {
            val viewModel: MappingViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is MappingEffect.NavigateToValidate ->
                            nav.navigate(Routes.validate(effect.importId)) {
                                // Mapping is locked once validation starts.
                                popUpTo(Routes.MAPPING) { inclusive = true }
                            }
                        MappingEffect.NavigateBack -> nav.popBackStack()
                    }
                }
            }

            MappingScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(Routes.VALIDATE, arguments = listOf(importIdArg)) { entry ->
            val importId = entry.arguments?.getString(ApiParams.PATH_IMPORT_ID).orEmpty()
            val viewModel: ValidationViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        ValidationEffect.NavigateToPreview ->
                            nav.navigate(Routes.preview(importId))
                        ValidationEffect.NavigateToUpload ->
                            nav.popBackStack(Routes.UPLOAD, inclusive = false)
                    }
                }
            }

            ValidationScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(Routes.PREVIEW, arguments = listOf(importIdArg)) {
            val viewModel: PreviewViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is PreviewEffect.NavigateToExecute ->
                            nav.navigate(Routes.result(effect.importId)) {
                                popUpTo(Routes.PREVIEW) { inclusive = true }
                            }
                        PreviewEffect.NavigateBack -> nav.popBackStack()
                    }
                }
            }

            PreviewScreen(state = state, onEvent = viewModel::onEvent)
        }

        composable(Routes.RESULT, arguments = listOf(importIdArg)) {
            val viewModel: ResultViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Once the import has executed there is no valid "previous" screen — the backend
            // rejects preview/validate on an executed job (409 conflict). System Back mirrors
            // Done: close the whole flow instead of returning to the stale validation screen.
            BackHandler { viewModel.onEvent(ResultEvent.DoneClicked) }

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        ResultEffect.NavigateToUpload ->
                            nav.popBackStack(Routes.UPLOAD, inclusive = false)
                        // Close the whole import flow; we land on whoever opened it.
                        ResultEffect.NavigateToHome -> {
                            onImportDone()
                            nav.popBackStack(Routes.IMPORT_GRAPH, inclusive = true)
                        }
                    }
                }
            }

            ResultScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
}