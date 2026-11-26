package com.yasminhosam.brandora.feature.auth.presentation.reset

import com.yasminhosam.brandora.core.network.*
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ResetOtpViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)

    private class Fake : AuthRepository {
        var resets = 0
        var sends = 0
        var arguments: List<String>? = null
        var resetResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var sendResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun register(email: String, password: String): ApiResult<AuthSession> =
            error("unused")

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
            error("unused")

        override suspend fun refresh(): ApiResult<AuthSession> = error("unused")

        override suspend fun forgotPassword(email: String): ApiResult<Unit> {
            sends++
            return sendResult
        }

        override suspend fun resetPassword(
            email: String,
            otp: String,
            newPassword: String,
        ): ApiResult<Unit> {
            resets++
            arguments = listOf(email, otp, newPassword)
            gate?.await()
            return resetResult
        }
    }

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    private fun model(f: Fake) = ResetViewModel(ResetPasswordUseCase(f), ForgotPasswordUseCase(f))

    private fun fill(v: ResetViewModel, otp: String = "01234567") {
        v.onEvent(ResetEvent.EmailChanged(" Owner@Example.com "))
        v.onEvent(ResetEvent.OtpChanged(otp))
        v.onEvent(ResetEvent.PasswordChanged(" NewPassword123! "))
    }

    @Test
    fun `leading zero preserved email normalized and password never trimmed`() =
        runTest(scheduler) {
            val f = Fake()
            val v = model(f)
            fill(v)
            v.onEvent(ResetEvent.SubmitClicked)
            assertEquals(listOf("owner@example.com", "01234567", " NewPassword123! "), f.arguments)
            assertEquals("", v.uiState.value.otp)
            assertEquals("", v.uiState.value.password)
        }

    @Test
    fun `Arabic digits rejected before network`() =
        runTest(scheduler) {
            val f = Fake()
            val v = model(f)
            fill(v, "٠١٢٣٤٥٦٧")
            v.onEvent(ResetEvent.SubmitClicked)
            assertEquals(0, f.resets)
            assertTrue(v.uiState.value.otpFormatInvalid)
        }

    @Test
    fun `short code rejected without sending`() =
        runTest(scheduler) {
            val f = Fake()
            val v = model(f)
            fill(v, "1234567")
            v.onEvent(ResetEvent.SubmitClicked)
            assertEquals(0, f.resets)
            assertTrue(v.uiState.value.otpFormatInvalid)
        }

    @Test
    fun `invalid email and weak password never sent`() =
        runTest(scheduler) {
            val f = Fake()
            val v = model(f)
            fill(v)
            v.onEvent(ResetEvent.EmailChanged("bad"))
            v.onEvent(ResetEvent.SubmitClicked)
            assertEquals(0, f.resets)
            assertNotNull(v.uiState.value.emailError)
            v.onEvent(ResetEvent.EmailChanged("a@example.com"))
            v.onEvent(ResetEvent.PasswordChanged("weak"))
            v.onEvent(ResetEvent.SubmitClicked)
            assertEquals(0, f.resets)
            assertNotNull(v.uiState.value.passwordError)
        }

    @Test
    fun `duplicate reset taps and input changes ignored during flight`() =
        runTest(scheduler) {
            val f = Fake()
            f.gate = CompletableDeferred()
            val v = model(f)
            fill(v)
            v.onEvent(ResetEvent.SubmitClicked)
            v.onEvent(ResetEvent.SubmitClicked)
            v.onEvent(ResetEvent.EmailChanged("different@example.com"))
            assertEquals(1, f.resets)
            assertEquals(" Owner@Example.com ", v.uiState.value.email)
            f.gate!!.complete(Unit)
        }

    @Test
    fun `invalid code gets generic rejection and no automatic retry`() =
        runTest(scheduler) {
            val f = Fake()
            f.resetResult =
                ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = "AUTH_RESET_INVALID")
            val v = model(f)
            fill(v)
            v.onEvent(ResetEvent.SubmitClicked)
            assertTrue(v.uiState.value.invalidCode)
            assertNull(v.uiState.value.submitError)
            assertEquals(1, f.resets)
            assertEquals(0, f.sends)
        }

    @Test
    fun `network failure does not automatically resend or reset`() =
        runTest(scheduler) {
            val f = Fake()
            f.resetResult = ApiResult.Error(ApiErrorType.TIMEOUT)
            val v = model(f)
            fill(v)
            v.onEvent(ResetEvent.SubmitClicked)
            assertEquals(1, f.resets)
            assertEquals(0, f.sends)
            assertEquals(ApiErrorType.TIMEOUT, v.uiState.value.submitError)
        }

    @Test
    fun `explicit resend clears old code and gates resend but not submit`() =
        runTest(scheduler) {
            val f = Fake()
            val v = model(f)
            fill(v)
            v.onEvent(ResetEvent.ResendClicked)
            assertEquals(1, f.sends)
            assertEquals("", v.uiState.value.otp)
            assertTrue(v.uiState.value.resendAcknowledged)
            assertNotNull(v.uiState.value.resendCooldown)
            // The resend wait must not block submitting a code the user already has.
            v.onEvent(ResetEvent.OtpChanged("01234567"))
            assertTrue(v.uiState.value.canSubmit)
            v.onEvent(ResetEvent.ResendClicked)
            assertEquals(1, f.sends)
            advanceTimeBy(31000)
            runCurrent()
            assertNull(v.uiState.value.resendCooldown)
            assertTrue(v.uiState.value.canResend)
        }

    @Test
    fun `a rate limit shows the countdown without a stacked error`() =
        runTest(scheduler) {
            val f = Fake()
            f.resetResult = ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED, retryAfterSeconds = 4)
            val v = model(f)
            fill(v)
            v.onEvent(ResetEvent.SubmitClicked)
            assertNull(v.uiState.value.submitError)
            assertEquals(4L, v.uiState.value.cooldown?.seconds)
        }

    @Test
    fun `rate limit honors retry after and blocks attempts`() =
        runTest(scheduler) {
            val f = Fake()
            f.resetResult = ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED, retryAfterSeconds = 4)
            val v = model(f)
            fill(v)
            v.onEvent(ResetEvent.SubmitClicked)
            assertEquals(4L, v.uiState.value.cooldown?.seconds)
            assertFalse(v.uiState.value.canSubmit)
            advanceTimeBy(4001)
            runCurrent()
            assertTrue(v.uiState.value.canSubmit)
        }

    @Test
    fun `unavailable mail is not falsely acknowledged`() =
        runTest(scheduler) {
            val f = Fake()
            f.sendResult = ApiResult.Error(ApiErrorType.AUTH_EMAIL_UNAVAILABLE)
            val v = model(f)
            fill(v)
            v.onEvent(ResetEvent.ResendClicked)
            assertFalse(v.uiState.value.resendAcknowledged)
            assertEquals(ApiErrorType.AUTH_EMAIL_UNAVAILABLE, v.uiState.value.submitError)
        }
}
