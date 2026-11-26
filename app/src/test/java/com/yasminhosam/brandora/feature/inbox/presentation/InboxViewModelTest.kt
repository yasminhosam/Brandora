package com.yasminhosam.brandora.feature.inbox.presentation

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.inbox.FakeInboxRepository
import com.yasminhosam.brandora.feature.inbox.conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.PagedList
import com.yasminhosam.brandora.feature.inbox.domain.usecase.GetConversationsUseCase
import com.yasminhosam.brandora.feature.inbox.presentation.inbox.InboxEvent
import com.yasminhosam.brandora.feature.inbox.presentation.inbox.InboxFilter
import com.yasminhosam.brandora.feature.inbox.presentation.inbox.InboxViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    private val repo = FakeInboxRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = InboxViewModel(GetConversationsUseCase(repo))

    @Test
    fun startsWithOpenFilter_andLoadsFirstPage() {
        repo.conversationsResult = { _, _ -> ApiResult.Success(PagedList(listOf(conversation("1")), 1, null)) }
        val vm = vm()
        assertEquals(listOf<Pair<ConversationStatus?, Int>>(ConversationStatus.OPEN to 0), repo.conversationCalls)
        assertEquals(1, vm.uiState.value.conversations.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun filterChange_reloadsWithTheNewStatus_andAllOmitsIt() {
        val vm = vm()
        vm.onEvent(InboxEvent.FilterSelected(InboxFilter.CLOSED))
        vm.onEvent(InboxEvent.FilterSelected(InboxFilter.ALL))
        assertEquals(listOf(ConversationStatus.OPEN, ConversationStatus.CLOSED, null), repo.conversationCalls.map { it.first })
    }

    @Test
    fun loadMore_appendsWithoutDuplicates() {
        repo.conversationsResult = { _, offset ->
            if (offset == 0) ApiResult.Success(PagedList(listOf(conversation("1"), conversation("2")), 3, 2))
            else ApiResult.Success(PagedList(listOf(conversation("2"), conversation("3")), 3, null))
        }
        val vm = vm()
        vm.onEvent(InboxEvent.LoadMoreClicked)
        assertEquals(listOf("1", "2", "3"), vm.uiState.value.conversations.map { it.id })
        assertNull(vm.uiState.value.nextOffset)
    }

    @Test
    fun firstLoadError_showsError_thenRetryRecovers() {
        repo.conversationsResult = { _, _ -> ApiResult.Error(ApiErrorType.NETWORK) }
        val vm = vm()
        assertNotNull(vm.uiState.value.errorRes)
        repo.conversationsResult = { _, _ -> ApiResult.Success(PagedList(listOf(conversation("1")), 1, null)) }
        vm.onEvent(InboxEvent.RetryClicked)
        assertNull(vm.uiState.value.errorRes)
        assertEquals(1, vm.uiState.value.conversations.size)
    }

    @Test
    fun failedRefresh_keepsTheExistingList() {
        repo.conversationsResult = { _, _ -> ApiResult.Success(PagedList(listOf(conversation("1")), 1, null)) }
        val vm = vm()
        repo.conversationsResult = { _, _ -> ApiResult.Error(ApiErrorType.TIMEOUT) }
        vm.onEvent(InboxEvent.RefreshRequested)
        assertEquals(1, vm.uiState.value.conversations.size)
        assertNull(vm.uiState.value.errorRes)
        assertNotNull(vm.uiState.value.loadMoreErrorRes)
        assertFalse(vm.uiState.value.isRefreshing)
    }
}
