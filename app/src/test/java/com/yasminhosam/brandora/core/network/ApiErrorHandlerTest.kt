package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.messageRes
import java.io.IOException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException

class ApiErrorHandlerTest {

    // Mirrors the app's shared Json (see NetworkModule).
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val handler = ApiErrorHandler(json)

    private fun httpException(
        code: Int,
        body: String = "",
        retryAfter: String? = null
    ): HttpException {
        val request = okhttp3.Request.Builder().url("http://localhost/resource").build()
        var builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("mock")
        if (retryAfter != null) builder = builder.header("Retry-After", retryAfter)
        val raw = builder.body("{}".toResponseBody("application/json".toMediaType())).build()
        // error(content, raw) is the only factory that keeps custom raw-response headers;
        // the content becomes the errorBody ApiErrorHandler parses.
        val content = body.toResponseBody("application/json".toMediaType())
        return HttpException(retrofit2.Response.error<Any>(content, raw))
    }

    @Test
    fun `429 maps to AUTH_RATE_LIMITED with a numeric Retry-After`() {
        val result = handler.handle(httpException(429, retryAfter = "30"))

        assertEquals(ApiErrorType.AUTH_RATE_LIMITED, result.type)
        assertEquals(429, result.code)
        assertEquals(30L, result.retryAfterSeconds)
    }

    @Test
    fun `429 without Retry-After still maps to AUTH_RATE_LIMITED`() {
        val result = handler.handle(httpException(429))

        assertEquals(ApiErrorType.AUTH_RATE_LIMITED, result.type)
        assertNull(result.retryAfterSeconds)
    }

    @Test
    fun `an HTTP-date Retry-After is treated as absent (numeric seconds only)`() {
        val result = handler.handle(httpException(429, retryAfter = "Wed, 21 Oct 2026 07:28:00 GMT"))

        assertEquals(ApiErrorType.AUTH_RATE_LIMITED, result.type)
        assertNull(result.retryAfterSeconds)
    }

    @Test
    fun `AUTH_RATE_LIMITED has a localized message`() {
        assertEquals(R.string.error_auth_rate_limited, ApiErrorType.AUTH_RATE_LIMITED.messageRes())
    }

    @Test
    fun `401 keeps the backend's stable code and ignores Retry-After`() {
        val body = """{"statusCode":401,"code":"AUTH_REFRESH_INVALID","message":"x","error":"Unauthorized"}"""
        val result = handler.handle(httpException(401, body, retryAfter = "5"))

        assertEquals(ApiErrorType.UNAUTHORIZED, result.type)
        assertEquals(AuthErrorCodes.AUTH_REFRESH_INVALID, result.errorCode)
        assertNull(result.retryAfterSeconds)
    }

    @Test
    fun `an IOException maps to NETWORK and a timeout to TIMEOUT`() {
        assertEquals(ApiErrorType.NETWORK, handler.handle(IOException("offline")).type)
        assertEquals(ApiErrorType.TIMEOUT, handler.handle(java.net.SocketTimeoutException()).type)
    }
}
