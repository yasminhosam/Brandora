package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches "Authorization: <tokenType> <accessToken>" to every outgoing request, using the
 * in-memory session (no disk read per request). Public /auth routes are left untouched.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val session = sessionStore.session.value

        val authenticated = if (session == null || request.url.isPublicAuthPath()) {
            request
        } else {
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, session.authorizationHeader)
                .build()
        }
        return chain.proceed(authenticated)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
