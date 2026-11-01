package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.model.AuthUser
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.usecase.RefreshSessionUseCase
import com.yasminhosam.brandora.feature.auth.testing.FakeSessionStore
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises TokenAuthenticator.authenticate() directly - the exact entry point OkHttp uses
 * on a 401. (An in-JVM client test cannot reach the authenticator: OkHttp invokes it below
 * the application-interceptor layer, which needs a real socket; the single-flight mechanics
 * are covered by SessionRefresherTest.)
 *
 * The follow-up protocol under test: authenticate(request) -> tagged request; if THAT
 * request ever 401s again, authenticate must refuse (at most one retry per request).
 */
class TokenAuthenticatorTest {

    private class FakeAuthRepository(
        private val store: FakeSessionStore,
        private val onRefresh: suspend () -> ApiResult<AuthSession>
    ) : AuthRepository {
        val refreshCalls = AtomicInteger(0)

        override suspend fun register(email: String, password: String): ApiResult<AuthSession> =
            error("not under test")

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
            error("not under test")

        override suspend fun refresh(): ApiResult<AuthSession> {
            refreshCalls.incrementAndGet()
            return onRefresh()
        }
    }

    private fun authenticator(
        store: FakeSessionStore,
        repository: FakeAuthRepository
    ) = TokenAuthenticator(SessionRefresher(RefreshSessionUseCase(repository), store))

    /** The 401 response OkHttp would hand to the authenticator for [request]. */
    private fun response401(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(401)
        .message("Unauthorized")
        .body("""{"statusCode":401}""".toResponseBody("application/json".toMediaType()))
        .build()

    private fun authenticate(
        authenticator: TokenAuthenticator,
        request: Request
    ): Request? = authenticator.authenticate(null, response401(request))

    private fun session(accessToken: String, refreshToken: String) = AuthSession(
        user = AuthUser(id = "u1"),
        accessToken = accessToken,
        refreshToken = refreshToken
    )

    private fun authenticatedRequest(authorization: String?): Request {
        val builder = Request.Builder().url("http://localhost:3000/businesses")
        if (authorization != null) builder.header("Authorization", authorization)
        return builder.build()
    }

    private fun rotated(store: FakeSessionStore): suspend () -> ApiResult<AuthSession> = {
        val newSession = session("at-new", "rt-new")
        store.save(newSession)
        ApiResult.Success(newSession)
    }

    @Test
    fun `a 401 refreshes once and the follow-up carries the rotated token`() {
        val store = FakeSessionStore(session("at-old", "rt-old"))
        val repository = FakeAuthRepository(store, rotated(store))
        val followUp = authenticate(
            authenticator(store, repository),
            authenticatedRequest("Bearer at-old")
        )

        assertNotNull(followUp)
        assertEquals("Bearer at-new", followUp!!.header("Authorization"))
        assertEquals(1, repository.refreshCalls.get())
        assertEquals("rt-new", store.session.value?.refreshToken)
    }

    @Test
    fun `the follow-up is tagged - a second 401 never refreshes or retries again`() {
        val store = FakeSessionStore(session("at-old", "rt-old"))
        val repository = FakeAuthRepository(store, rotated(store))
        val authenticator = authenticator(store, repository)

        val followUp = authenticate(authenticator, authenticatedRequest("Bearer at-old"))
        // The backend 401s AGAIN on the tagged follow-up: must give up, not loop.
        val second = followUp?.let { authenticate(authenticator, it) }

        assertNull(second)
        assertEquals(1, repository.refreshCalls.get())
    }

    @Test
    fun `a token that already rotated skips the refresh entirely`() {
        val store = FakeSessionStore(session("at-new", "rt-new"))
        val repository = FakeAuthRepository(store) { error("must not refresh") }
        val followUp = authenticate(
            authenticator(store, repository),
            authenticatedRequest("Bearer at-old") // this caller still held the stale token
        )

        assertNotNull(followUp)
        assertEquals("Bearer at-new", followUp!!.header("Authorization"))
        assertEquals(0, repository.refreshCalls.get())
    }

    @Test
    fun `a 401 on a public auth route never triggers a refresh`() {
        val store = FakeSessionStore(session("at-old", "rt-old"))
        val repository = FakeAuthRepository(store) { error("must not refresh") }
        val request = Request.Builder().url("http://localhost:3000/auth/login")
            .header("Authorization", "Bearer at-old")
            .build()

        assertNull(authenticate(authenticator(store, repository), request))
        assertEquals(0, repository.refreshCalls.get())
    }

    @Test
    fun `a 401 without a stored session fails without refreshing`() {
        val store = FakeSessionStore(null)
        val repository = FakeAuthRepository(store) { error("must not refresh") }

        assertNull(
            authenticate(authenticator(store, repository), authenticatedRequest(null))
        )
        assertEquals(0, repository.refreshCalls.get())
    }

    @Test
    fun `a rate-limited refresh gives up but keeps the session`() {
        val store = FakeSessionStore(session("at-old", "rt-old"))
        val repository = FakeAuthRepository(store) {
            ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED, retryAfterSeconds = 30)
        }

        assertNull(
            authenticate(authenticator(store, repository), authenticatedRequest("Bearer at-old"))
        )
        assertEquals(1, repository.refreshCalls.get())
        assertEquals("at-old", store.session.value?.accessToken)
        assertEquals(0, store.clears)
    }

    @Test
    fun `a session cleared during the 401 handling is not resurrected`() {
        val store = FakeSessionStore(session("at-old", "rt-old"))
        val repository = FakeAuthRepository(store) {
            // Mirrors AuthRepositoryImpl: lost response -> clear, never retry the old token.
            store.clear()
            ApiResult.Error(ApiErrorType.NETWORK)
        }

        assertNull(
            authenticate(authenticator(store, repository), authenticatedRequest("Bearer at-old"))
        )
        assertEquals(1, repository.refreshCalls.get())
        assertNull(store.session.value)
    }

    @Test
    fun `the retry marker is a request tag - tags are never serialized onto the wire`() {
        val store = FakeSessionStore(session("at-old", "rt-old"))
        val repository = FakeAuthRepository(store, rotated(store))
        val followUp = authenticate(
            authenticator(store, repository),
            authenticatedRequest("Bearer at-old")
        )!!

        // The marker exists (proved by the second-401 test) but no header carries it.
        assertTrue(followUp.headers.names().none { it.equals("X-Retried", true) })
        // The Authorization header is the only auth-related header on the follow-up.
        assertEquals(1, followUp.headers.size)
    }
}
