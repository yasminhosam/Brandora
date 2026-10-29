package com.yasminhosam.brandora.feature.auth.presentation.login

import com.yasminhosam.brandora.core.navigation.Routes
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.model.AuthUser
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.usecase.LoginUseCase
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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

    private val mainDispatcher = UnconfinedTestDispatcher()

    private class FakeAuthRepository(
        private val result: ApiResult<AuthSession>,
        /** When set, login suspends until completed - keeps a submit actually "in flight". */
        private val gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    ) : AuthRepository {
        var calls = 0
            private set

        var lastEmail: String? = null
            private set

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> {
            calls++
            lastEmail = email
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

    @Test
    fun `a successful login lands on Home when a business is known`() = runTest {
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
    fun `login without a business lands on Business Create`() = runTest {
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
    fun `a blank field never reaches the backend`() = runTest {
        val repository = FakeAuthRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.onEvent(LoginEvent.EmailChanged("a@b.c"))
        viewModel.onEvent(LoginEvent.PasswordChanged("   "))
        viewModel.onEvent(LoginEvent.SubmitClicked)

        assertEquals(0, repository.calls)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `INVALID_CREDENTIALS shows the inline message instead of the generic error`() = runTest {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.UNAUTHORIZED, errorCode = AuthErrorCodes.INVALID_CREDENTIALS)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.onEvent(LoginEvent.EmailChanged("a@b.c"))
        viewModel.onEvent(LoginEvent.PasswordChanged("wrong"))
        viewModel.onEvent(LoginEvent.SubmitClicked)

        val state = viewModel.uiState.value
        assertTrue(state.invalidCredentials)
        assertNull(state.submitError)
        assertFalse(state.isSubmitting)
        assertEquals(1, repository.calls)
    }

    @Test
    fun `a non-credential failure surfaces the typed error`() = runTest {
        val repository = FakeAuthRepository(ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED, retryAfterSeconds = 30))
        val viewModel = viewModel(repository = repository)

        viewModel.onEvent(LoginEvent.EmailChanged("a@b.c"))
        viewModel.onEvent(LoginEvent.PasswordChanged("secret"))
        viewModel.onEvent(LoginEvent.SubmitClicked)

        val state = viewModel.uiState.value
        assertEquals(ApiErrorType.AUTH_RATE_LIMITED, state.submitError)
        assertFalse(state.invalidCredentials)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `editing after a rejected attempt clears the credentials note`() = runTest {
        val repository = FakeAuthRepository(
            ApiResult.Error(ApiErrorType.UNAUTHORIZED, errorCode = AuthErrorCodes.INVALID_CREDENTIALS)
        )
        val viewModel = viewModel(repository = repository)

        viewModel.onEvent(LoginEvent.EmailChanged("a@b.c"))
        viewModel.onEvent(LoginEvent.PasswordChanged("wrong"))
        viewModel.onEvent(LoginEvent.SubmitClicked)
        viewModel.onEvent(LoginEvent.PasswordChanged("fixed"))

        assertFalse(viewModel.uiState.value.invalidCredentials)
    }

    @Test
    fun `double submit while in flight is ignored`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val repository = FakeAuthRepository(SUCCESS, gate)
        val viewModel = viewModel(repository = repository)

        viewModel.onEvent(LoginEvent.EmailChanged("a@b.c"))
        viewModel.onEvent(LoginEvent.PasswordChanged("secret"))
        viewModel.onEvent(LoginEvent.SubmitClicked)
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
