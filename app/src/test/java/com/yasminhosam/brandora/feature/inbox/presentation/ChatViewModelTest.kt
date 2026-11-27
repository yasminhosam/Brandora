package com.yasminhosam.brandora.feature.inbox.presentation

import androidx.lifecycle.SavedStateHandle
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.inbox.FakeInboxRepository
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatRules
import com.yasminhosam.brandora.feature.inbox.domain.model.PagedList
import com.yasminhosam.brandora.feature.inbox.domain.usecase.GetConversationUseCase
import com.yasminhosam.brandora.feature.inbox.domain.usecase.GetMessagesUseCase
import com.yasminhosam.brandora.feature.inbox.domain.usecase.SendMessageUseCase
import com.yasminhosam.brandora.feature.inbox.domain.usecase.SetAiEnabledUseCase
import com.yasminhosam.brandora.feature.inbox.message
import com.yasminhosam.brandora.feature.inbox.presentation.chat.ChatEvent
import com.yasminhosam.brandora.feature.inbox.presentation.chat.ChatViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val repo = FakeInboxRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = ChatViewModel(
        GetConversationUseCase(repo),
        GetMessagesUseCase(repo),
        SendMessageUseCase(repo),
        SetAiEnabledUseCase(repo),
        SavedStateHandle(mapOf(ApiParams.CONVERSATION_ID to "c1"))
    )

    @Test
    fun loadsHeaderAndFirstPage_newestFirst() {
        repo.messagesResult = { ApiResult.Success(PagedList(listOf(message("2"), message("1")), 2, null)) }
        val s = vm().uiState.value
        assertNotNull(s.conversation)
        assertEquals(listOf("2", "1"), s.messages.map { it.id })
        assertFalse(s.isLoading)
        assertFalse(s.hasOlder)
    }

    @Test
    fun headerFailure_showsErrorEvenIfMessagesLoaded() {
        repo.conversationResult = ApiResult.Error(ApiErrorType.NOT_FOUND)
        repo.messagesResult = { ApiResult.Success(PagedList(listOf(message("1")), 1, null)) }
        val s = vm().uiState.value
        assertNotNull(s.errorRes)
        assertNull(s.conversation)
    }

    @Test
    fun loadOlder_appendsAtTheEnd_withoutDuplicates() {
        repo.messagesResult = { offset ->
            if (offset == 0) ApiResult.Success(PagedList(listOf(message("3"), message("2")), 4, 2))
            else ApiResult.Success(PagedList(listOf(message("2"), message("1")), 4, null))
        }
        val vm = vm()
        assertTrue(vm.uiState.value.hasOlder)
        vm.onEvent(ChatEvent.LoadOlderClicked)
        assertEquals(listOf("3", "2", "1"), vm.uiState.value.messages.map { it.id })
        assertFalse(vm.uiState.value.hasOlder)
        assertEquals(listOf(0, 2), repo.messageOffsets)
    }

    @Test
    fun headerClick_opensAndDismissClosesSheet() {
        val vm = vm()
        vm.onEvent(ChatEvent.HeaderClicked)
        assertTrue(vm.uiState.value.showCustomerSheet)
        vm.onEvent(ChatEvent.SheetDismissed)
        assertFalse(vm.uiState.value.showCustomerSheet)
    }

    @Test
    fun send_prependsTheStoredMessage_clearsDraft_andTurnsAiOff() {
        repo.messagesResult = { ApiResult.Success(PagedList(listOf(message("1")), 1, null)) }
        val vm = vm()

        vm.onEvent(ChatEvent.ReplyChanged("  Hello  "))
        assertTrue(vm.uiState.value.canSend)
        vm.onEvent(ChatEvent.SendClicked)

        val state = vm.uiState.value
        assertEquals(listOf("Hello"), repo.sentMessages)
        assertEquals("sent", state.messages.first().id)
        assertEquals(listOf("sent", "1"), state.messages.map { it.id })
        assertEquals("", state.draft)
        assertFalse(state.isSending)
        assertNull(state.sendErrorRes)
        assertFalse(state.conversation!!.aiEnabled)
    }

    @Test
    fun send_failureKeepsTheDraftAndExplainsTheWindow() {
        repo.sendResult = {
            ApiResult.Error(ApiErrorType.CONFLICT, errorCode = "WHATSAPP_WINDOW_CLOSED")
        }
        val vm = vm()

        vm.onEvent(ChatEvent.ReplyChanged("Hello"))
        vm.onEvent(ChatEvent.SendClicked)

        val state = vm.uiState.value
        assertEquals("Hello", state.draft)
        assertEquals(R.string.chat_error_window_closed, state.sendErrorRes)
        assertFalse(state.isSending)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun send_ignoresBlankAndOverlongReplies() {
        val vm = vm()
        vm.onEvent(ChatEvent.SendClicked)

        vm.onEvent(ChatEvent.ReplyChanged("x".repeat(ChatRules.MAX_MESSAGE_CODE_POINTS + 1)))
        assertTrue(vm.uiState.value.draftTooLong)
        assertFalse(vm.uiState.value.canSend)
        vm.onEvent(ChatEvent.SendClicked)

        assertTrue(repo.sentMessages.isEmpty())
    }

    @Test
    fun retrySend_resendsWithoutTouchingTheDraft() {
        val vm = vm()
        vm.onEvent(ChatEvent.ReplyChanged("a draft"))
        vm.onEvent(ChatEvent.RetrySendClicked("older text"))

        assertEquals(listOf("older text"), repo.sentMessages)
        assertEquals("a draft", vm.uiState.value.draft)
    }

    @Test
    fun aiToggle_sendsTheRequestedValue() {
        val vm = vm()
        vm.onEvent(ChatEvent.AiToggled(false))

        assertEquals(listOf(false), repo.aiCalls)
        assertFalse(vm.uiState.value.conversation!!.aiEnabled)
        assertFalse(vm.uiState.value.isTogglingAi)
        assertNull(vm.uiState.value.aiErrorRes)
    }

    @Test
    fun aiToggle_failureKeepsTheStateAndShowsTheError() {
        repo.aiResult = { ApiResult.Error(ApiErrorType.SERVER) }
        val vm = vm()

        vm.onEvent(ChatEvent.AiToggled(false))

        val state = vm.uiState.value
        assertTrue(state.conversation!!.aiEnabled)
        assertEquals(R.string.error_server, state.aiErrorRes)
        assertFalse(state.isTogglingAi)
    }
}
