package com.yasminhosam.brandora.feature.inbox.presentation.navigation

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
import com.yasminhosam.brandora.feature.inbox.presentation.chat.ChatEffect
import com.yasminhosam.brandora.feature.inbox.presentation.chat.ChatScreen
import com.yasminhosam.brandora.feature.inbox.presentation.chat.ChatViewModel
import com.yasminhosam.brandora.feature.inbox.presentation.inbox.InboxEffect
import com.yasminhosam.brandora.feature.inbox.presentation.inbox.InboxScreen
import com.yasminhosam.brandora.feature.inbox.presentation.inbox.InboxViewModel
import com.yasminhosam.brandora.feature.inbox.presentation.openWhatsApp

private val conversationIdArg = navArgument(ApiParams.CONVERSATION_ID) { type = NavType.StringType }

/** Nested inbox graph: conversation list -> chat. Opened from a Home card; not a bottom tab. */
fun NavGraphBuilder.inboxGraph(nav: NavHostController) {
    navigation(startDestination = Routes.INBOX, route = Routes.INBOX_GRAPH) {

        composable(Routes.INBOX) {
            val viewModel: InboxViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is InboxEffect.OpenChat -> nav.navigate(Routes.chat(effect.conversationId))
                    }
                }
            }

            InboxScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onBackClick = { nav.popBackStack() }
            )
        }

        composable(Routes.CHAT, arguments = listOf(conversationIdArg)) {
            val viewModel: ChatViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is ChatEffect.OpenWhatsApp -> openWhatsApp(context, effect.phone)
                    }
                }
            }

            ChatScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onBackClick = { nav.popBackStack() }
            )
        }
    }
}
