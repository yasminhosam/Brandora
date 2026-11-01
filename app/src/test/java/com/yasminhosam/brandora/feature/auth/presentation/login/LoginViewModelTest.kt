package com.yasminhosam.brandora.feature.auth.presentation.login

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
import com.yasminhosam.brandora.feature.auth.domain.usecase.LoginUseCase
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError
import com.yasminhosam.brandora.feature.auth.domain.validation.PASSWORD_MAX_LENGTH
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
class LoginViewModelTest {

    // One scheduler drives Main and runTest so the cooldown's virtual delays are testable.
    private val scheduler = TestCoroutineScheduler()
    private val mainDispatcher = UnconfinedTestDispatcher(scheduler)

    private class FakeAuthRepository(
        private val result: ApiResult<AuthSession>,
        /** When set, login suspends until completed - keeps a submit actually "in flight". */
        private val gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    ) : AuthRepository {
        var calls = 0
            private set

        var lastEmail: String? = null
            private set

        var lastPassword: String? = null
            private set

        override suspend fun register(email: String, password: String): ApiResult<AuthSession> =
            error("not under test")

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> {
            calls++
            lastEmail = email
            lastPassword = password
            gate?.await()
            return result
        }

        override suspend fun refresh(): ApiResult<AuthSession> = error("not under test")
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
        businessId: String? = "biz-1"
    ) = LoginViewModel(
        login = LoginUseCase(repository),
        currentBusinessStore = FakeBusinessStore(businessId)
    )

    private fun LoginViewModel.submit(email: String, password: String) {
        onEvent(LoginEvent.EmailChanged(email))
        onEvent(LoginEvent.PasswordChanged(password))
        onEvent(LoginEvent.SubmitClicked)
    }

    @Test
    fun `a successful login lands on Home when a business is known`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository, businessId = "biz-1")
        val effects = mutableListOf<LoginEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(LoginEvent.EmailChanged(" shop@dates.example "))
        viewModel.onEvent(LoginEvent.PasswordChanged("secret"))
        viewModel.onEvent(LoginEvent.SubmitClicked)

        // Trimmed before it reaches the backend.
        assertEquals("shop@dates.example", repository.lastEmail)
        assertEquals(listOf<LoginEffect>(LoginEffect.LoginSucceeded(Routes.HOME)), effects)
        assertFalse(viewModel.uiState.value.isSubmitting)
        collection.cancel()
    }

    @Test
    fun `login without a business lands on Business Create`() = runTest(scheduler) {
        val viewModel = viewModel(businessId = null)
        val effects = mutableListOf<LoginEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(LoginEvent.EmailChanged("a@b.c"))
        viewModel.onEvent(LoginEvent.PasswordChanged("secret"))
        viewModel.onEvent(LoginEvent.SubmitClicked)

        assertEquals(
            listOf<LoginEffect>(LoginEffect.LoginSucceeded(Routes.BUSINESS_CREATE)),
            effects
        )
        collection.cancel()
    }

    @Test
    fun `a blank field never reaches the backend`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.onEvent(LoginEvent.EmailChanged("a@b.c"))
        viewModel.onEvent(LoginEvent.PasswordChanged("   "))
        viewModel.onEvent(LoginEvent.SubmitClicked)

        assertEquals(0, repository.calls)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `an invalid email never reaches the backend`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.submit("not-an-email", "secret")

        assertEquals(0, repository.calls)
        assertEquals(AuthEmailError.INVALID, viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `a too-long login password is rejected locally`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "p".repeat(PASSWORD_MAX_LENGTH + 1))

        assertEquals(0, repository.calls)
        assertEquals(AuthPasswordError.TOO_LONG, viewModel.uiState.value.passwordError)
    }

    @Test
    fun `the email is normalized before the request`() = runTest(scheduler) {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.submit("  Shop@Dates.Example  ", "secret")

        assertEquals("shop@dates.example", repository.lastEmail)
    }

    @Test
    fun `the password is cleared from state on success`() = runTest(scheduler) {
        val viewModel = viewModel()

        viewModel.submit("a@b.c", "secret")

        assertEquals("", viewModel.uiState.value.password)
    }

    @Test
    fun `blurring an empty field marks it required`() = runTest(scheduler) {
        val viewModel = viewModel()

        viewModel.onEvent(LoginEvent.EmailFocusLost)
        viewModel.onEvent(LoginEvent.PasswordFocusLost)

        val state = viewModel.uiState.value
        assertEquals(AuthEmailError.REQUIRED, state.emailError)
        assertEquals(AuthPasswordError.REQUIRED, state.passwordError)
    }

    @Test
    fun `erasing a filled field marks it required`() = runTest(scheduler) {
        val viewModel = viewModel()

        viewModel.onEvent(LoginEvent.EmailChanged("a@b.c"))
        viewModel.onEvent(LoginEvent.EmailChanged(""))

        assertEquals(AuthEmailError.REQUIRED, viewModel.uiState.value.emailError)
    }

    @Test
    fun `INVALID_CREDENTIALS shows the inline message instead of the generic error`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.UNAUTHORIZED, errorCode = AuthErrorCodes.INVALID_CREDENTIALS)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "wrong")

        val state = viewModel.uiState.value
        assertTrue(state.invalidCredentials)
        assertNull(state.submitError)
        assertFalse(state.isSubmitting)
        assertEquals(1, repository.calls)
    }

    @Test
    fun `VALIDATION_ERROR shows the generic invalid-input message`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = AuthErrorCodes.VALIDATION_ERROR)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "secret")

        val state = viewModel.uiState.value
        assertEquals(ApiErrorType.BAD_REQUEST, state.submitError)
        // The shared generic copy: no backend text is ever shown.
        assertEquals(R.string.error_bad_request, state.submitError?.messageRes())
    }

    @Test
    fun `a non-credential failure surfaces the typed error`() = runTest(scheduler) {
        val repository = FakeAuthRepository(ApiResult.Error(ApiErrorType.SERVER))
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "secret")

        val state = viewModel.uiState.value
        assertEquals(ApiErrorType.SERVER, state.submitError)
        assertFalse(state.invalidCredentials)
        assertFalse(state.isSubmitting)
        assertNull(state.cooldown)
    }

    @Test
    fun `a rate-limited login disables submit until the cooldown ends`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED, retryAfterSeconds = 5)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "secret")

        val cooling = viewModel.uiState.value
        assertEquals(5L, cooling.cooldown?.seconds)
        assertEquals(ApiErrorType.AUTH_RATE_LIMITED, cooling.cooldown?.type)
        assertFalse(cooling.canSubmit)
        assertNull(cooling.submitError)

        // Submit is refused for the whole window...
        viewModel.onEvent(LoginEvent.SubmitClicked)
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

        viewModel.submit("a@b.c", "secret")

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
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "secret")

        val cooldown = viewModel.uiState.value.cooldown
        assertEquals(10L, cooldown?.seconds)
        assertEquals(ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE, cooldown?.type)
        scheduler.advanceUntilIdle()
    }

    @Test
    fun `editing after a rejected attempt clears the credentials note`() = runTest(scheduler) {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.UNAUTHORIZED, errorCode = AuthErrorCodes.INVALID_CREDENTIALS)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "wrong")
        viewModel.onEvent(LoginEvent.PasswordChanged("fixed"))

        assertFalse(viewModel.uiState.value.invalidCredentials)
    }

    @Test
    fun `double submit while in flight is ignored`() = runTest(scheduler) {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val repository = FakeAuthRepository(SUCCESS, gate)
        val viewModel = viewModel(repository = repository)

        viewModel.submit("a@b.c", "secret")
        // Still suspended inside login: the state is mid-flight for the second click.
        viewModel.onEvent(LoginEvent.SubmitClicked)

        assertEquals(1, repository.calls)
        assertTrue(viewModel.uiState.value.isSubmitting)

        gate.complete(Unit)
    }

    private companion object {
        val SUCCESS = ApiResult.Success(
            AuthSession(
                user = AuthUser(id = "u1", email = "a@b.c"),
                accessToken = "at",
                refreshToken = "rt"
            )
        )
    }
}
