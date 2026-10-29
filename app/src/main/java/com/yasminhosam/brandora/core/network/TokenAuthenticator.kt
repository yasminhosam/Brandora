package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Reacts to a 401 on an authenticated route by refreshing once and retrying the request
 * exactly once (a request tagged [AuthRetry] is never retried again, so a backend that keeps
 * answering 401 cannot loop).
 *
 * The refresh itself is serialized by SessionRefresher; a failed refresh returns null and
 * the 401 is delivered to the caller. [authenticate] is synchronous by contract, so it
 * blocks on the refresher - bounded by the auth client's short timeouts.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val refresher: SessionRefresher
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request

        // A 401 from a public route is a credentials problem, not an expired token.
        if (request.url.isPublicAuthPath()) return null

        // At most one retry per request.
        if (request.tag(AuthRetry::class.java) != null) return null

        // "<tokenType> <accessToken>" -> the bare token; the scheme casing is not trusted.
        val failedToken = request.header(AUTHORIZATION_HEADER)
            ?.substringAfter(' ', missingDelimiterValue = "")
            ?.ifBlank { null }

        val newToken = try {
            runBlocking { refresher.validAccessToken(failedToken) }
        } catch (e: CancellationException) {
            throw e
        } ?: return null

        return request.newBuilder()
            .tag(AuthRetry::class.java, AuthRetry)
            .header(AUTHORIZATION_HEADER, "${AuthSession.DEFAULT_TOKEN_TYPE} $newToken")
            .build()
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}

/** Request tag marking a post-refresh retry; never sent on the wire. */
private object AuthRetry
