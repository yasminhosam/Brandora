package com.yasminhosam.brandora.feature.catalogimport.presentation.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
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
import com.yasminhosam.brandora.feature.catalogimport.presentation.validation.ValidationEvent
import com.yasminhosam.brandora.feature.catalogimport.presentation.validation.ValidationScreen
import com.yasminhosam.brandora.feature.catalogimport.presentation.validation.ValidationViewModel

private val importIdArg = navArgument(ApiParams.PATH_IMPORT_ID) { type = NavType.StringType }

/** Must match the placeholder in [Routes.IMPORT_GRAPH]. */
private const val ONBOARDING_ARG = "onboarding"

/**
 * True only for the automatic first-run entry straight after business creation. Read from the
 * graph's own back stack entry, which is on the stack while any of its screens is showing.
 */
private fun NavHostController.isOnboardingImport(): Boolean =
    getBackStackEntry(Routes.IMPORT_GRAPH).arguments?.getBoolean(ONBOARDING_ARG) == true

/**
 * Nested import graph: Upload → Mapping → Validate → Preview → Result. [onImportDone] fires when
 * the import flow finishes successfully and is popping back to whichever tab opened it (used for
 * the completion snackbar).
 */
fun NavGraphBuilder.importGraph(nav: NavHostController, onImportDone: () -> Unit = {}) {
    navigation(
        startDestination = Routes.UPLOAD,
        route = Routes.IMPORT_GRAPH,
        // True only for the automatic first-run entry after business creation.
        arguments =
            listOf(navArgument(ONBOARDING_ARG) { type = NavType.BoolType; defaultValue = false }),
    ) {
        composable(Routes.UPLOAD) { backStackEntry ->
            val viewModel: UploadViewModel = hiltViewModel()
            LaunchedEffect(Unit) { viewModel.onEvent(UploadEvent.ScreenEntered) }
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            // First run (straight after business creation) is the only entry that offers Skip;
            // the Home and Products entries are deliberate visits the merchant can just leave.
            val onboarding = remember(backStackEntry) { nav.isOnboardingImport() }

            val picker =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
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
                        // Close the import flow; if in onboarding, continue to working hours.
                        UploadEffect.NavigateToNextStep -> {
                            nav.popBackStack(Routes.IMPORT_GRAPH, inclusive = true)
                            if (onboarding) {
                                nav.navigate(Routes.workingHours(onboarding = true))
                            }
                        }
                    }
                }
            }

            UploadScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onBrowseClick = { picker.launch(arrayOf("*/*")) },
                // Leaving from Products (or Home) returns to that screen, not to Upload.
                onBack = { nav.popBackStack(Routes.IMPORT_GRAPH, inclusive = true) },
                showSkip = onboarding,
            )
        }

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

            // Re-entry from Preview must re-arm the Continue navigation latch.
            LaunchedEffect(Unit) { viewModel.onEvent(ValidationEvent.ScreenEntered) }

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        ValidationEffect.NavigateToPreview -> nav.navigate(Routes.preview(importId))
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

        composable(Routes.RESULT, arguments = listOf(importIdArg)) { backStackEntry ->
            val viewModel: ResultViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Only the first-run entry continues to working hours. An import opened from Home or
            // Products closes the flow back to that screen instead.
            val onboarding = remember(backStackEntry) { nav.isOnboardingImport() }

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
                            if (onboarding) {
                                nav.navigate(Routes.workingHours(onboarding = true))
                            }
                        }
                    }
                }
            }

            ResultScreen(state = state, onEvent = viewModel::onEvent)
        }
    }
}
