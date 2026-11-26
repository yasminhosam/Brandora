package com.yasminhosam.brandora.feature.home

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.inbox.FakeInboxRepository
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.PagedList
import com.yasminhosam.brandora.feature.inbox.domain.usecase.GetOpenChatCountUseCase
import com.yasminhosam.brandora.feature.home.presentation.HomeEvent
import com.yasminhosam.brandora.feature.home.presentation.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val repo = FakeInboxRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun badge_usesTotalCountOfOpenChats_withOneItemPage() {
        repo.conversationsResult = { _, _ -> ApiResult.Success(PagedList(emptyList(), 7, null)) }
        val vm = HomeViewModel(GetOpenChatCountUseCase(repo))
        assertNull(vm.uiState.value.openChatCount)
        vm.onEvent(HomeEvent.Refresh)
        assertEquals(7, vm.uiState.value.openChatCount)
        assertEquals(listOf<Pair<ConversationStatus?, Int>>(ConversationStatus.OPEN to 0), repo.conversationCalls)
    }

    @Test
    fun failedRefresh_keepsLastKnownCount() {
        repo.conversationsResult = { _, _ -> ApiResult.Success(PagedList(emptyList(), 3, null)) }
        val vm = HomeViewModel(GetOpenChatCountUseCase(repo))
        vm.onEvent(HomeEvent.Refresh)
        repo.conversationsResult = { _, _ -> ApiResult.Error(ApiErrorType.NETWORK) }
        vm.onEvent(HomeEvent.Refresh)
        assertEquals(3, vm.uiState.value.openChatCount)
    }
}
