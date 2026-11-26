package com.yasminhosam.brandora.feature.workinghours.presentation.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.feature.workinghours.presentation.WorkingHoursEffect
import com.yasminhosam.brandora.feature.workinghours.presentation.WorkingHoursNav
import com.yasminhosam.brandora.feature.workinghours.presentation.WorkingHoursScreen
import com.yasminhosam.brandora.feature.workinghours.presentation.WorkingHoursViewModel

/**
 * One screen, two entry points: after the import (onboarding=true, skippable) and from the
 * Home card (onboarding=false). [onSaved] shows the root snackbar, which survives the pop.
 */
fun NavGraphBuilder.workingHoursScreen(nav: NavHostController, onSaved: () -> Unit = {}) {
    composable(
        route = Routes.WORKING_HOURS,
        arguments = listOf(
            navArgument(WorkingHoursNav.ARG_ONBOARDING) {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) {
        val viewModel: WorkingHoursViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    WorkingHoursEffect.Saved -> {
                        onSaved()
                        nav.popBackStack()
                    }
                    WorkingHoursEffect.AlreadyConfigured -> nav.popBackStack()
                }
            }
        }

        WorkingHoursScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onBackClick = { nav.popBackStack() },
            onSkipClick = { nav.popBackStack() }
        )
    }
}