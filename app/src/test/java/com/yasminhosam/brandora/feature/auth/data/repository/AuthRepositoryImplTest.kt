package com.yasminhosam.brandora.feature.auth.data.repository

import com.yasminhosam.brandora.core.network.ApiErrorHandler
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.core.network.SessionExpiredSignal
import com.yasminhosam.brandora.feature.auth.data.remote.AuthRemoteDataSource
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.model.AuthUser
import com.yasminhosam.brandora.feature.auth.testing.FakeSessionStore
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Exercises the session-persistence rules of AuthRepositoryImpl against a fake backend
 * (an OkHttp interceptor fabricates responses; no extra test libraries).
 */
class AuthRepositoryImplTest {

    // Mirrors the app's shared Json (see NetworkModule).
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private class FakeBackend : Interceptor {
        /** Return null from [respond] to simulate a lost response (connection died). */
        var respond: ((Request) -> Response?)? = null
        var requests = 0
            private set

        override fun intercept(chain: Interceptor.Chain): Response {
            requests++
            return respond?.invoke(chain.request())
                ?: throw IOException("connection reset (response lost)")
        }
    }

    private fun repository(
        backend: FakeBackend,
        store: FakeSessionStore,
        signal: SessionExpiredSignal = SessionExpiredSignal()
    ): Triple<AuthRepositoryImpl, FakeSessionStore, SessionExpiredSignal> {
        val client = OkHttpClient.Builder().addInterceptor(backend).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:3000/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val remote = AuthRemoteDataSource(retrofit, ApiErrorHandler(json))
        return Triple(AuthRepositoryImpl(remote, store, signal), store, signal)
    }

    private fun sessionResponse(accessToken: String, refreshToken: String): String = """
        {
          "user": {"id": "u1", "email": "a@b.c"},
          "accessToken": "$accessToken",
          "refreshToken": "$refreshToken",
          "tokenType": "Bearer",
          "expiresIn": 900,
          "refreshExpiresAt": "2026-10-30T09:00:00Z"
        }
    """.trimIndent()

    private fun errorBody(statusCode: Int, code: String): String =
        """{"statusCode":$statusCode,"code":"$code","message":"x","error":"Unauthorized"}"""

    private fun response(request: Request, code: Int, body: String = "", retryAfter: String? = null): Response {
        var builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("mock")
        if (retryAfter != null) builder = builder.header("Retry-After", retryAfter)
        return builder.body(body.toResponseBody("application/json".toMediaType())).build()
    }

    @Test
    fun `a successful login persists the session`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request -> response(request, 200, sessionResponse("at-1", "rt-1")) }
        }
        val store = FakeSessionStore()
        val (repository, _, _) = repository(backend, store)

        val result = repository.login("a@b.c", "secret")

        assertTrue(result is ApiResult.Success)
        assertEquals("at-1", store.session.value?.accessToken)
        assertEquals("rt-1", store.session.value?.refreshToken)
        assertEquals(1, store.saves)
    }

    @Test
    fun `a failed login leaves any existing session untouched`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request ->
                response(request, 401, errorBody(401, AuthErrorCodes.INVALID_CREDENTIALS))
            }
        }
        val store = FakeSessionStore(OLD)
        val (repository, _, _) = repository(backend, store)

        val result = repository.login("a@b.c", "wrong")

        assertTrue(result is ApiResult.Error)
        assertEquals(AuthErrorCodes.INVALID_CREDENTIALS, (result as ApiResult.Error).errorCode)
        assertEquals(OLD.accessToken, store.session.value?.accessToken)
        assertEquals(0, store.saves)
        assertEquals(0, store.clears)
    }

    @Test
    fun `a successful refresh rotates and persists the tokens`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request -> response(request, 200, sessionResponse("at-2", "rt-2")) }
        }
        val store = FakeSessionStore(OLD)
        val (repository, _, _) = repository(backend, store)

        val result = repository.refresh()

        assertTrue(result is ApiResult.Success)
        assertEquals("at-2", store.session.value?.accessToken)
        assertEquals("rt-2", store.session.value?.refreshToken)
        assertEquals(0, store.clears)
    }

    @Test
    fun `AUTH_REFRESH_INVALID clears the session and emits the expired signal`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request -> response(request, 401, errorBody(401, AuthErrorCodes.AUTH_REFRESH_INVALID)) }
        }
        val store = FakeSessionStore(OLD)
        val signal = SessionExpiredSignal()
        val expired = mutableListOf<Unit>()
        val collector = launch {
            signal.events.collect { expired += it }
        }

        val (repository, _, _) = repository(backend, store, signal)
        val result = repository.refresh()

        assertTrue(result is ApiResult.Error)
        assertEquals(1, store.clears)
        assertNull(store.session.value)
        kotlinx.coroutines.yield() // let the tryEmit reach the collector

        assertEquals(1, expired.size)
        collector.cancel()
    }

    @Test
    fun `a lost refresh response clears the session (never retry the old token)`() = runBlocking {
        val backend = FakeBackend().apply { respond = null } // every call throws IOException
        val store = FakeSessionStore(OLD)
        val signal = SessionExpiredSignal()
        val (repository, _, _) = repository(backend, store, signal)

        val result = repository.refresh()

        assertTrue(result is ApiResult.Error)
        assertEquals(com.yasminhosam.brandora.core.network.ApiErrorType.NETWORK, (result as ApiResult.Error).type)
        assertEquals(1, store.clears)
        assertNull(store.session.value)
    }

    @Test
    fun `a rate-limited refresh keeps the session`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request ->
                response(request, 429, errorBody(429, AuthErrorCodes.AUTH_RATE_LIMITED), retryAfter = "30")
            }
        }
        val store = FakeSessionStore(OLD)
        val signal = SessionExpiredSignal()
        val (repository, _, _) = repository(backend, store, signal)

        val result = repository.refresh()

        assertEquals(com.yasminhosam.brandora.core.network.ApiErrorType.AUTH_RATE_LIMITED, (result as ApiResult.Error).type)
        assertEquals(30L, result.retryAfterSeconds)
        assertEquals(0, store.clears)
        assertEquals(OLD.accessToken, store.session.value?.accessToken)
    }

    @Test
    fun `refresh without a session fails fast and never hits the backend`() = runBlocking {
        val backend = FakeBackend()
        val store = FakeSessionStore(null)
        val (repository, _, _) = repository(backend, store)

        val result = repository.refresh()

        assertEquals(com.yasminhosam.brandora.core.network.ApiErrorType.UNAUTHORIZED, (result as ApiResult.Error).type)
        assertEquals(0, backend.requests)
        assertEquals(0, store.clears)
    }

    @Test
    fun `the refresh request carries the stored refresh token`() = runBlocking {
        var refreshBody: String? = null
        val backend = FakeBackend().apply {
            respond = { request ->
                refreshBody = request.body?.let { body ->
                    okio.Buffer().also { body.writeTo(it) }.readUtf8()
                }
                response(request, 200, sessionResponse("at-2", "rt-2"))
            }
        }
        val store = FakeSessionStore(OLD)
        val (repository, _, _) = repository(backend, store)

        repository.refresh()

        assertNotNull(refreshBody)
        assertTrue(refreshBody!!.contains("\"refreshToken\":\"refresh-old\""))
    }

    @Test
    fun `a successful register persists the session (201)`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request -> response(request, 201, sessionResponse("at-1", "rt-1")) }
        }
        val store = FakeSessionStore()
        val (repository, _, _) = repository(backend, store)

        val result = repository.register("a@b.c", "Secret1Abcdefg")

        assertTrue(result is ApiResult.Success)
        assertEquals("at-1", store.session.value?.accessToken)
        assertEquals("rt-1", store.session.value?.refreshToken)
        assertEquals(1, store.saves)
    }

    @Test
    fun `a rejected registration leaves any existing session untouched`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request ->
                response(request, 409, errorBody(409, AuthErrorCodes.EMAIL_UNAVAILABLE))
            }
        }
        val store = FakeSessionStore(OLD)
        val (repository, _, _) = repository(backend, store)

        val result = repository.register("taken@b.c", "Secret1Abcdefg")

        assertTrue(result is ApiResult.Error)
        assertEquals(AuthErrorCodes.EMAIL_UNAVAILABLE, (result as ApiResult.Error).errorCode)
        assertEquals(OLD.accessToken, store.session.value?.accessToken)
        assertEquals(0, store.saves)
        assertEquals(0, store.clears)
    }

    @Test
    fun `a successful forgot-password request never touches the session`() = runBlocking {
        // The 200 body is the backend's generic message: parsed by Retrofit's Unit
        // converter (closed, never read) and never shown to the user.
        val backend = FakeBackend().apply {
            respond = { request ->
                response(request, 200, """{"statusCode":200,"message":"If an account exists..."}""")
            }
        }
        val store = FakeSessionStore(OLD)
        val (repository, _, _) = repository(backend, store)

        val result = repository.forgotPassword("a@b.c")

        assertTrue(result is ApiResult.Success)
        assertEquals(0, store.saves)
        assertEquals(0, store.clears)
        assertEquals(OLD.accessToken, store.session.value?.accessToken)
    }

    @Test
    fun `a successful reset (204) revokes the local session`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request -> response(request, 204) } // No Content: empty body
        }
        val store = FakeSessionStore(OLD)
        val (repository, _, _) = repository(backend, store)

        val result = repository.resetPassword("tok-1", "Secret1Abcdefg")

        assertTrue(result is ApiResult.Success)
        assertEquals(1, store.clears)
        assertNull(store.session.value)
    }

    @Test
    fun `a rejected reset keeps the session (only a 204 revokes it)`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request ->
                response(request, 400, errorBody(400, AuthErrorCodes.AUTH_RESET_INVALID))
            }
        }
        val store = FakeSessionStore(OLD)
        val (repository, _, _) = repository(backend, store)

        val result = repository.resetPassword("expired", "Secret1Abcdefg")

        assertTrue(result is ApiResult.Error)
        assertEquals(AuthErrorCodes.AUTH_RESET_INVALID, (result as ApiResult.Error).errorCode)
        assertEquals(0, store.clears)
        assertEquals(OLD.accessToken, store.session.value?.accessToken)
    }

    @Test
    fun `a rate-limited reset keeps the session and carries Retry-After`() = runBlocking {
        val backend = FakeBackend().apply {
            respond = { request ->
                response(request, 429, errorBody(429, AuthErrorCodes.AUTH_RATE_LIMITED), retryAfter = "30")
            }
        }
        val store = FakeSessionStore(OLD)
        val (repository, _, _) = repository(backend, store)

        val result = repository.resetPassword("tok-1", "Secret1Abcdefg")

        val error = result as ApiResult.Error
        assertEquals(com.yasminhosam.brandora.core.network.ApiErrorType.AUTH_RATE_LIMITED, error.type)
        assertEquals(30L, error.retryAfterSeconds)
        assertEquals(0, store.clears)
        assertEquals(OLD.accessToken, store.session.value?.accessToken)
    }

    @Test
    fun `the forgot and reset requests carry exactly the contract fields`() = runBlocking {
        val bodies = mutableMapOf<String, String>()
        val backend = FakeBackend().apply {
            respond = { request ->
                request.body?.let { body ->
                    val buffer = okio.Buffer()
                    body.writeTo(buffer)
                    bodies[request.url.encodedPath] = buffer.readUtf8()
                }
                if (request.url.encodedPath.endsWith("forgot-password")) {
                    response(request, 200, """{"statusCode":200}""")
                } else {
                    response(request, 204)
                }
            }
        }
        val store = FakeSessionStore()
        val (repository, _, _) = repository(backend, store)

        repository.forgotPassword("a@b.c")
        repository.resetPassword("tok-1", "Secret1Abcdefg")

        assertEquals("""{"email":"a@b.c"}""", bodies["/auth/forgot-password"])
        assertEquals(
            """{"token":"tok-1","newPassword":"Secret1Abcdefg"}""",
            bodies["/auth/reset-password"]
        )
    }

    private companion object {
        val OLD = AuthSession(
            user = AuthUser(id = "u1"),
            accessToken = "access-old",
            refreshToken = "refresh-old"
        )
    }
}
