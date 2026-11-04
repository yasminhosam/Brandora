package com.yasminhosam.brandora.feature.auth.domain.model

import kotlinx.serialization.Serializable

/** The authenticated user inside a session. Only the fields the app may one day display. */
@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
    val name: String? = null
)

/**
 * The session returned by login/refresh: tokens plus the user they belong to.
 * expiresIn/refreshExpiresAt from the contract are intentionally not modeled - nothing uses
 * them yet (refresh is reactive, on 401), and the shared Json ignores unknown keys.
 */
@Serializable
data class AuthSession(
    val user: AuthUser,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = DEFAULT_TOKEN_TYPE
) {
    /** The Authorization header value, honoring the backend's tokenType with a Bearer fallback. */
    val authorizationHeader: String
        get() = "${tokenType.ifBlank { DEFAULT_TOKEN_TYPE }} $accessToken"

    companion object {
        const val DEFAULT_TOKEN_TYPE = "Bearer"
    }
}
