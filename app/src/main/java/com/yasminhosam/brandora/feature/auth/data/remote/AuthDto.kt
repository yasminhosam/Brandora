package com.yasminhosam.brandora.feature.auth.data.remote

import kotlinx.serialization.Serializable

@Serializable
internal data class UserDto(
    val id: String,
    val email: String? = null,
    val name: String? = null
)

/** The session payload shared by /auth/login, /auth/refresh (and the future auth routes). */
@Serializable
internal data class SessionDto(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long? = null,          // stored-only for now: refresh is reactive (on 401)
    val refreshExpiresAt: String? = null
)

/** The {email, password} body shared by POST /auth/login and POST /auth/register. */
@Serializable
internal data class CredentialsRequestDto(
    val email: String,
    val password: String
)

/** The {email} body of POST /auth/forgot-password (public; the reply never confirms delivery). */
@Serializable
internal data class ForgotPasswordRequestDto(
    val email: String
)

/** The {token, newPassword} body of POST /auth/reset-password; the token comes from the app link. */
@Serializable
internal data class ResetPasswordRequestDto(
    val token: String,
    val newPassword: String
)

@Serializable
internal data class RefreshRequestDto(
    val refreshToken: String
)
