package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.model.AuthUser
import com.yasminhosam.brandora.feature.auth.testing.FakeSessionStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
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

class AuthInterceptorTest {

    /** Terminal interceptor: records the outgoing request and answers 200. */
    private class Recorder : Interceptor {
        var seen: Request? = null
        override fun intercept(chain: Interceptor.Chain): Response {
            seen = chain.request()
            return okhttp3.Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    private fun execute(store: FakeSessionStore, path: String): Request {
        val recorder = Recorder()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store))
            .addInterceptor(recorder)
            .build()
        client.newCall(Request.Builder().url("http://localhost:3000/$path").build()).execute()
        return requireNotNull(recorder.seen)
    }

    @Test
    fun `attaches the Bearer header from the in-memory session`() {
        val request = execute(FakeSessionStore(SESSION), "catalog-items")

        assertEquals("Bearer access-1", request.header("Authorization"))
    }

    @Test
    fun `honors the backend's tokenType with a Bearer fallback`() {
        val request = execute(FakeSessionStore(SESSION.copy(tokenType = "bearer")), "catalog-items")

        assertEquals("bearer access-1", request.header("Authorization"))
    }

    @Test
    fun `public auth routes never carry the header`() {
        val request = execute(FakeSessionStore(SESSION), "auth/login")

        assertNull(request.header("Authorization"))
    }

    @Test
    fun `no session means no header`() {
        val request = execute(FakeSessionStore(null), "businesses")

        assertNull(request.header("Authorization"))
    }

    @Test
    fun `public-path detection also matches a prefixed base URL`() {
        val request = execute(FakeSessionStore(SESSION), "api/v2/auth/refresh")

        assertNull(request.header("Authorization"))
        assertNotNull(request)
        assertTrue(request.url.encodedPath.endsWith(ApiEndpoints.AUTH_REFRESH))
    }

    private companion object {
        val SESSION = AuthSession(
            user = AuthUser(id = "u1", email = "a@b.c"),
            accessToken = "access-1",
            refreshToken = "refresh-1"
        )
    }
}
