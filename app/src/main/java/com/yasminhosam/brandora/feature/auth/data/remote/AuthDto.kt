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

@Serializable
internal data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
internal data class RefreshRequestDto(
    val refreshToken: String
)
