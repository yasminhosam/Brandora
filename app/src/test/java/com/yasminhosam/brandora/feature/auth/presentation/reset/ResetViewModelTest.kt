package com.yasminhosam.brandora.feature.auth.presentation.reset

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.usecase.ResetPasswordUseCase
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResetViewModelTest {

    // One scheduler drives Main and runTest so the cooldown's virtual delays are testable.
    private val scheduler = TestCoroutineScheduler()
    private val mainDispatcher = UnconfinedTestDispatcher(scheduler)

    private class FakeAuthRepository(
        private val result: ApiResult<Unit>,
        /** When set, reset suspends until completed - keeps a submit actually "in flight". */
        private val gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    ) : AuthRepository {
        var calls = 0
            private set

        var lastToken: String? = null
            private set

        var lastPassword: String? = null
            private set

        override suspend fun register(email: String, password: String): ApiResult<AuthSession> =
            error("not under test")

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
            error("not under test")

        override suspend fun refresh(): ApiResult<AuthSession> = error("not under test")

        override suspend fun forgotPassword(email: String): ApiResult<Unit> =
            error("not under test")

        override suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit> {
            calls++
            lastToken = token
            lastPassword = newPassword
            gate?.await()
            return result
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeAuthRepository) = ResetViewModel(
        resetPassword = ResetPasswordUseCase(repository)
    )

    private fun ResetViewModel.typePassword(password: String) {
        onEvent(ResetEvent.PasswordChanged(password))
    }

    @Test
    fun `a successful reset clears both secrets and signals success`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)
        val effects = mutableListOf<ResetEffect>()
        val collection = launch(mainDispatcher) {
            viewModel.effects.collect { effects += it }
        }

        viewModel.onEvent(ResetEvent.TokenArrived("tok-1"))
        viewModel.typePassword("Secret1Abcdefg")
        viewModel.onEvent(ResetEvent.SubmitClicked)

        val state = viewModel.uiState.value
        assertEquals(listOf<ResetEffect>(ResetEffect.ResetSucceeded), effects)
        // The token and password lived only in memory: both are gone after the request.
        assertNull(state.token)
        assertEquals("", state.password)
        assertFalse(state.isSubmitting)
        assertEquals(1, repository.calls)
        collection.cancel()
    }

    @Test
    fun `the request carries the token and the untrimmed password`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)

        viewModel.onEvent(ResetEvent.TokenArrived("tok-1"))
        viewModel.typePassword(" Secret1Abcdefg ") // spaces are real characters (register rule)
        viewModel.onEvent(ResetEvent.SubmitClicked)

        assertEquals("tok-1", repository.lastToken)
        assertEquals(" Secret1Abcdefg ", repository.lastPassword)
    }

    @Test
    fun `a too-short password never reaches the backend`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)

        viewModel.onEvent(ResetEvent.TokenArrived("tok-1"))
        viewModel.typePassword("Short1")
        viewModel.onEvent(ResetEvent.SubmitClicked)

        assertEquals(0, repository.calls)
        assertEquals(AuthPasswordError.TOO_SHORT, viewModel.uiState.value.passwordError)
    }

    @Test
    fun `a password without the required complexity never reaches the backend`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)

        viewModel.onEvent(ResetEvent.TokenArrived("tok-1"))
        viewModel.typePassword("abcdefghijklmno") // no digit, no uppercase
        viewModel.onEvent(ResetEvent.SubmitClicked)

        assertEquals(0, repository.calls)
        assertEquals(AuthPasswordError.MISSING_COMPLEXITY, viewModel.uiState.value.passwordError)
    }

    @Test
    fun `AUTH_RESET_INVALID shows the dedicated invalid-link note`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = AuthErrorCodes.AUTH_RESET_INVALID)
        )
        val viewModel = viewModel(repository)

        viewModel.onEvent(ResetEvent.TokenArrived("expired"))
        viewModel.typePassword("Secret1Abcdefg")
        viewModel.onEvent(ResetEvent.SubmitClicked)

        val state = viewModel.uiState.value
        assertTrue(state.invalidLink)
        assertNull(state.submitError) // not the generic 400 copy
        assertFalse(state.isSubmitting)
        assertEquals(1, repository.calls)
    }

    @Test
    fun `submitting without a token shows the note and never sends anything`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)

        viewModel.typePassword("Secret1Abcdefg")
        viewModel.onEvent(ResetEvent.SubmitClicked)

        assertEquals(0, repository.calls)
        assertTrue(viewModel.uiState.value.invalidLink)
    }

    @Test
    fun `a newer link replaces the token and clears the stale invalid-link note`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = AuthErrorCodes.AUTH_RESET_INVALID)
        )
        val viewModel = viewModel(repository)

        viewModel.onEvent(ResetEvent.TokenArrived("old"))
        viewModel.typePassword("Secret1Abcdefg")
        viewModel.onEvent(ResetEvent.SubmitClicked)
        assertTrue(viewModel.uiState.value.invalidLink)

        viewModel.onEvent(ResetEvent.TokenArrived("fresh"))

        val state = viewModel.uiState.value
        assertEquals("fresh", state.token)
        assertFalse(state.invalidLink)
        assertNull(state.submitError)
    }

    @Test
    fun `a rate-limited reset disables submit until the cooldown ends`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED, retryAfterSeconds = 5)
        )
        val viewModel = viewModel(repository)

        viewModel.onEvent(ResetEvent.TokenArrived("tok-1"))
        viewModel.typePassword("Secret1Abcdefg")
        viewModel.onEvent(ResetEvent.SubmitClicked)

        val cooling = viewModel.uiState.value
        assertEquals(5L, cooling.cooldown?.seconds)
        assertFalse(cooling.canSubmit)

        // Submit is refused for the whole window...
        viewModel.onEvent(ResetEvent.SubmitClicked)
        assertEquals(1, repository.calls)

        // ...and comes back once the countdown reaches zero.
        scheduler.advanceTimeBy(6_000L)
        assertNull(viewModel.uiState.value.cooldown)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `double submit while in flight is ignored`() = runTest(scheduler) {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val repository = FakeAuthRepository(ApiResult.Success(Unit), gate)
        val viewModel = viewModel(repository)

        viewModel.onEvent(ResetEvent.TokenArrived("tok-1"))
        viewModel.typePassword("Secret1Abcdefg")
        viewModel.onEvent(ResetEvent.SubmitClicked)
        // Still suspended inside the request: the state is mid-flight for the second click.
        viewModel.onEvent(ResetEvent.SubmitClicked)

        assertEquals(1, repository.calls)
        assertTrue(viewModel.uiState.value.isSubmitting)

        gate.complete(Unit)
    }

    @Test
    fun `a network failure surfaces the typed generic error`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Error(ApiErrorType.NETWORK))
        val viewModel = viewModel(repository)

        viewModel.onEvent(ResetEvent.TokenArrived("tok-1"))
        viewModel.typePassword("Secret1Abcdefg")
        viewModel.onEvent(ResetEvent.SubmitClicked)

        val state = viewModel.uiState.value
        assertEquals(ApiErrorType.NETWORK, state.submitError)
        assertFalse(state.invalidLink)
        assertNull(state.cooldown)
    }

    @Test
    fun `blurring an empty password marks it required`() = runTest(scheduler) {
        val viewModel = viewModel(FakeAuthRepository(ApiResult.Success(Unit)))

        viewModel.onEvent(ResetEvent.PasswordFocusLost)

        assertEquals(AuthPasswordError.REQUIRED, viewModel.uiState.value.passwordError)
    }
}
