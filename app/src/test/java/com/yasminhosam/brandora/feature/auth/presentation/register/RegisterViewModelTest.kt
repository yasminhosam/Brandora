package com.yasminhosam.brandora.feature.auth.presentation.register

import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.model.AuthUser
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.usecase.RegisterUseCase
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class RegisterViewModelTest {

    // One scheduler drives Main and runTest so the cooldown's virtual delays are testable.
    private val scheduler = TestCoroutineScheduler()
    private val mainDispatcher = UnconfinedTestDispatcher(scheduler)

    private class FakeAuthRepository(
        private val result: ApiResult<AuthSession>,
        /** When set, register suspends until completed - keeps a submit actually "in flight". */
        private val gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    ) : AuthRepository {
        var calls = 0
            private set

        var lastEmail: String? = null
            private set

        var lastPassword: String? = null
            private set

        override suspend fun register(email: String, password: String): ApiResult<AuthSession> {
            calls++
            lastEmail = email
            lastPassword = password
            gate?.await()
            return result
        }

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
            error("not under test")

        override suspend fun refresh(): ApiResult<AuthSession> = error("not under test")

        override suspend fun forgotPassword(email: String): ApiResult<Unit> =
            error("not under test")

        override suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit> =
            error("not under test")
    }

    private class FakeBusinessStore(id: String?) : CurrentBusinessStore {
        override val businessId: Flow<String?> = MutableStateFlow(id)
        override suspend fun setBusinessId(id: String) = error("not under test")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeAuthRepository = FakeAuthRepository(SUCCESS),
        businessId: String? = null
    ) = RegisterViewModel(
        register = RegisterUseCase(repository),
        currentBusinessStore = FakeBusinessStore(businessId)
    )

    private fun RegisterViewModel.submit(email: String, password: String) {
        onEvent(RegisterEvent.EmailChanged(email))
        onEvent(RegisterEvent.PasswordChanged(password))
        onEvent(RegisterEvent.SubmitClicked)
    }

    @Test
    fun `a new account lands on Business Create`() = runTest(scheduler) {
        val viewModel = viewModel()
        val effects = mutableListOf<RegisterEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.submit("a@b.c", VALID_PASSWORD)

        assertEquals(
            listOf<RegisterEffect>(RegisterEffect.RegisterSucceeded(Routes.BUSINESS_CREATE)),
            effects
        )
        collection.cancel()
    }

    @Test
    fun `an account with a known business lands on Home`() = runTest(scheduler) {
        val viewModel = viewModel(businessId = "biz-1")
        val effects = mutableListOf<RegisterEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.submit("a@b.c", VALID_PASSWORD)

        assertEquals(
            listOf<RegisterEffect>(RegisterEffect.RegisterSucceeded(Routes.HOME)),
            effects
        )
        collection.cancel()
    }

    @Test
    fun `the password is cleared from state on success`() = runTest(scheduler) {
        val viewModel = viewModel()

        viewModel.submit("a@b.c", VALID_PASSWORD)

        assertEquals("", viewModel.uiState.value.password)
    }

    @Test
    fun `EMAIL_UNAVAILABLE shows on the email field instead of a generic error`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.CONFLICT, errorCode = AuthErrorCodes.EMAIL_UNAVAILABLE)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.submit("taken@b.c", VALID_PASSWORD)

        val state = viewModel.uiState.value
        assertEquals(AuthEmailError.EMAIL_UNAVAILABLE, state.emailError)
        assertNull(state.submitError)
        assertFalse(state.isSubmitting)
        assertEquals(1, repository.calls)
    }

    @Test
    fun `editing the email clears a stale EMAIL_UNAVAILABLE`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.CONFLICT, errorCode = AuthErrorCodes.EMAIL_UNAVAILABLE)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.submit("taken@b.c", VALID_PASSWORD)
        viewModel.onEvent(RegisterEvent.EmailChanged("other@b.c"))

        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `VALIDATION_ERROR shows the generic invalid-input message`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = AuthErrorCodes.VALIDATION_ERROR)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", VALID_PASSWORD)

        val state = viewModel.uiState.value
        assertEquals(ApiErrorType.BAD_REQUEST, state.submitError)
        // The shared generic copy: no backend text is ever shown.
        assertEquals(R.string.error_bad_request, state.submitError?.messageRes())
    }

    @Test
    fun `an invalid email never reaches the backend`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.submit("not-an-email", VALID_PASSWORD)

        assertEquals(0, repository.calls)
        assertEquals(AuthEmailError.INVALID, viewModel.uiState.value.emailError)
    }

    @Test
    fun `a short register password never reaches the backend`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "Abc1defghij") // 11 chars

        assertEquals(0, repository.calls)
        assertEquals(AuthPasswordError.TOO_SHORT, viewModel.uiState.value.passwordError)
    }

    @Test
    fun `a password missing a character class never reaches the backend`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "abcdefghijkl123") // lower + digits, no uppercase

        assertEquals(0, repository.calls)
        assertEquals(AuthPasswordError.MISSING_COMPLEXITY, viewModel.uiState.value.passwordError)
    }

    @Test
    fun `the register password is never trimmed`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        // Valid length INCLUDING the leading space - the backend must receive it as typed.
        viewModel.submit("a@b.c", " $VALID_PASSWORD")

        assertEquals(1, repository.calls)
        assertEquals(" $VALID_PASSWORD", repository.lastPassword)
    }

    @Test
    fun `the email is normalized before the request`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.submit("  USER@Example.COM ", VALID_PASSWORD)

        assertEquals("user@example.com", repository.lastEmail)
    }

    @Test
    fun `a rate-limited register disables submit until the cooldown ends`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED, retryAfterSeconds = 5)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", VALID_PASSWORD)

        val cooling = viewModel.uiState.value
        assertEquals(5L, cooling.cooldown?.seconds)
        assertFalse(cooling.canSubmit)

        // Submit is refused for the whole window...
        viewModel.onEvent(RegisterEvent.SubmitClicked)
        assertEquals(1, repository.calls)

        // ...and comes back once the countdown reaches zero.
        scheduler.advanceTimeBy(6_000L)
        assertNull(viewModel.uiState.value.cooldown)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `a rate limit without Retry-After falls back to 30 seconds`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED))
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", VALID_PASSWORD)

        assertEquals(
            ApiConfig.RATE_LIMIT_FALLBACK_SECONDS,
            viewModel.uiState.value.cooldown?.seconds
        )
        scheduler.advanceUntilIdle()
    }

    @Test
    fun `double submit while in flight is ignored`() = runTest(scheduler) {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val repository = FakeAuthRepository(SUCCESS, gate)
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", VALID_PASSWORD)
        // Still suspended inside register: the state is mid-flight for the second click.
        viewModel.onEvent(RegisterEvent.SubmitClicked)

        assertEquals(1, repository.calls)
        assertTrue(viewModel.uiState.value.isSubmitting)

        gate.complete(Unit)
    }

    private companion object {
        /** 13 chars: upper, lower and digit - the valid-password baseline for these tests. */
        const val VALID_PASSWORD = "Abcdefghijkl1"

        val SUCCESS = ApiResult.Success(
            AuthSession(
                user = AuthUser(id = "u1", email = "a@b.c"),
                accessToken = "at",
                refreshToken = "rt"
            )
        )
    }
}
