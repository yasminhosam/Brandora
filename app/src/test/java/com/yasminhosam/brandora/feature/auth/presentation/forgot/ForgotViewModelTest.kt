package com.yasminhosam.brandora.feature.auth.presentation.forgot

import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.usecase.ForgotPasswordUseCase
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class ForgotViewModelTest {

    // One scheduler drives Main and runTest so the cooldown's virtual delays are testable.
    private val scheduler = TestCoroutineScheduler()
    private val mainDispatcher = UnconfinedTestDispatcher(scheduler)

    private class FakeAuthRepository(
        private val result: ApiResult<Unit>,
        /** When set, forgot suspends until completed - keeps a submit actually "in flight". */
        private val gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    ) : AuthRepository {
        var calls = 0
            private set

        var lastEmail: String? = null
            private set

        override suspend fun register(email: String, password: String): ApiResult<AuthSession> =
            error("not under test")

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
            error("not under test")

        override suspend fun refresh(): ApiResult<AuthSession> = error("not under test")

        override suspend fun forgotPassword(email: String): ApiResult<Unit> {
            calls++
            lastEmail = email
            gate?.await()
            return result
        }

        override suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit> =
            error("not under test")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeAuthRepository) = ForgotViewModel(
        forgotPassword = ForgotPasswordUseCase(repository)
    )

    private fun ForgotViewModel.submit(email: String) {
        onEvent(ForgotEvent.EmailChanged(email))
        onEvent(ForgotEvent.SubmitClicked)
    }

    @Test
    fun `a successful request shows the generic success state`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)

        viewModel.submit("shop@dates.example")

        val state = viewModel.uiState.value
        // One flag for every address: the state can never distinguish known from unknown.
        assertTrue(state.isSubmitted)
        assertNull(state.submitError)
        assertNull(state.emailError)
        assertFalse(state.isSubmitting)
        assertEquals(1, repository.calls)
    }

    @Test
    fun `the success state disables any further submit`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)

        viewModel.submit("a@b.c")
        viewModel.onEvent(ForgotEvent.SubmitClicked)

        assertEquals(1, repository.calls)
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `an invalid email never reaches the backend`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)

        viewModel.submit("not-an-email")

        assertEquals(0, repository.calls)
        assertEquals(AuthEmailError.INVALID, viewModel.uiState.value.emailError)
    }

    @Test
    fun `a blank email never reaches the backend`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)

        viewModel.onEvent(ForgotEvent.SubmitClicked)

        assertEquals(0, repository.calls)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `the email is normalized before the request`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Success(Unit))
        val viewModel = viewModel(repository)

        viewModel.submit("  Shop@Dates.Example  ")

        assertEquals("shop@dates.example", repository.lastEmail)
    }

    @Test
    fun `a rate-limited request disables submit until the cooldown ends`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED, retryAfterSeconds = 5)
        )
        val viewModel = viewModel(repository)

        viewModel.submit("a@b.c")

        val cooling = viewModel.uiState.value
        assertEquals(5L, cooling.cooldown?.seconds)
        assertEquals(ApiErrorType.AUTH_RATE_LIMITED, cooling.cooldown?.type)
        assertFalse(cooling.canSubmit)

        // Submit is refused for the whole window...
        viewModel.onEvent(ForgotEvent.SubmitClicked)
        assertEquals(1, repository.calls)

        // ...and comes back once the countdown reaches zero.
        scheduler.advanceTimeBy(6_000L)
        assertNull(viewModel.uiState.value.cooldown)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `a rate limit without Retry-After falls back to 30 seconds`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED))
        val viewModel = viewModel(repository)

        viewModel.submit("a@b.c")

        assertEquals(
            ApiConfig.RATE_LIMIT_FALLBACK_SECONDS,
            viewModel.uiState.value.cooldown?.seconds
        )
        scheduler.advanceUntilIdle()
    }

    @Test
    fun `the 503-unavailable contract uses its own countdown type`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE, retryAfterSeconds = 10)
        )
        val viewModel = viewModel(repository)

        viewModel.submit("a@b.c")

        val cooldown = viewModel.uiState.value.cooldown
        assertEquals(10L, cooldown?.seconds)
        assertEquals(ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE, cooldown?.type)
        scheduler.advanceUntilIdle()
    }

    @Test
    fun `AUTH_EMAIL_UNAVAILABLE is a plain typed error, never a cooldown`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(
                ApiErrorType.AUTH_EMAIL_UNAVAILABLE,
                errorCode = AuthErrorCodes.AUTH_EMAIL_UNAVAILABLE
            )
        )
        val viewModel = viewModel(repository)

        viewModel.submit("a@b.c")

        val state = viewModel.uiState.value
        assertEquals(ApiErrorType.AUTH_EMAIL_UNAVAILABLE, state.submitError)
        assertNull(state.cooldown)
        assertTrue(state.canSubmit) // no Retry-After in that contract: usable again now
    }

    @Test
    fun `VALIDATION_ERROR shows the generic invalid-input message`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = AuthErrorCodes.VALIDATION_ERROR)
        )
        val viewModel = viewModel(repository)

        viewModel.submit("a@b.c")

        val state = viewModel.uiState.value
        assertEquals(ApiErrorType.BAD_REQUEST, state.submitError)
        assertNull(state.cooldown)
    }

    @Test
    fun `double submit while in flight is ignored`() = runTest(scheduler) {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val repository = FakeAuthRepository(ApiResult.Success(Unit), gate)
        val viewModel = viewModel(repository)

        viewModel.submit("a@b.c")
        // Still suspended inside the request: the state is mid-flight for the second click.
        viewModel.onEvent(ForgotEvent.SubmitClicked)

        assertEquals(1, repository.calls)
        assertTrue(viewModel.uiState.value.isSubmitting)

        gate.complete(Unit)
    }

    @Test
    fun `blurring an empty field marks it required`() = runTest(scheduler) {
        val viewModel = viewModel(FakeAuthRepository(ApiResult.Success(Unit)))

        viewModel.onEvent(ForgotEvent.EmailFocusLost)

        assertEquals(AuthEmailError.REQUIRED, viewModel.uiState.value.emailError)
    }
}
