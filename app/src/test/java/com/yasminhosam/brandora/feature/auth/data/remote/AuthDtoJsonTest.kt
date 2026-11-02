package com.yasminhosam.brandora.feature.auth.data.remote

import com.yasminhosam.brandora.feature.auth.data.mapper.toDomain
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDtoJsonTest {

    // Mirrors the app's shared Json (see NetworkModule).
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `the session payload parses exactly as the contract describes`() {
        val domain = json.decodeFromString<SessionDto>(
            """
            {
              "user": {"id": "u1", "email": "a@b.c", "name": "Aya"},
              "accessToken": "at-1",
              "refreshToken": "rt-1",
              "tokenType": "Bearer",
              "expiresIn": 900,
              "refreshExpiresAt": "2026-10-30T09:00:00Z"
            }
            """.trimIndent()
        ).toDomain()

        assertEquals("u1", domain.user.id)
        assertEquals("a@b.c", domain.user.email)
        assertEquals("Aya", domain.user.name)
        assertEquals("at-1", domain.accessToken)
        assertEquals("rt-1", domain.refreshToken)
        assertEquals("Bearer", domain.tokenType)
        assertEquals("Bearer at-1", domain.authorizationHeader)
    }

    @Test
    fun `an unknown future field is ignored rather than crashing`() {
        val domain = json.decodeFromString<SessionDto>(
            """
            {
              "user": {"id": "u1"},
              "accessToken": "at-1",
              "refreshToken": "rt-1",
              "mfaRequired": true,
              "scopes": ["catalog:read"]
            }
            """.trimIndent()
        ).toDomain()

        assertEquals("at-1", domain.accessToken)
        assertNull(domain.user.email)
    }

    @Test
    fun `a missing tokenType falls back to Bearer`() {
        val domain = json.decodeFromString<SessionDto>(
            """
            {"user": {"id": "u1"}, "accessToken": "at", "refreshToken": "rt"}
            """.trimIndent()
        ).toDomain()

        assertEquals(AuthSession.DEFAULT_TOKEN_TYPE, domain.tokenType)
        assertEquals("Bearer at", domain.authorizationHeader)
    }

    @Test
    fun `a blank tokenType still falls back to Bearer in the header`() {
        val session = json.decodeFromString<SessionDto>(
            """
            {"user": {"id": "u1"}, "accessToken": "at", "refreshToken": "rt", "tokenType": ""}
            """.trimIndent()
        ).toDomain()

        assertEquals("", session.tokenType)
        assertEquals("Bearer at", session.authorizationHeader)
    }

    @Test
    fun `the refresh body carries only the refresh token`() {
        val encoded = json.encodeToString(
            RefreshRequestDto.serializer(),
            RefreshRequestDto(refreshToken = "rt-1")
        )

        assertEquals("""{"refreshToken":"rt-1"}""", encoded)
    }

    @Test
    fun `the credentials body matches the contract on both auth routes`() {
        val encoded = json.encodeToString(
            CredentialsRequestDto.serializer(),
            CredentialsRequestDto(email = "a@b.c", password = "secret")
        )

        // Exactly the two contract fields - nothing else may leak into the request.
        assertEquals("""{"email":"a@b.c","password":"secret"}""", encoded)
    }

    @Test
    fun `the forgot body carries only the email`() {
        val encoded = json.encodeToString(
            ForgotPasswordRequestDto.serializer(),
            ForgotPasswordRequestDto(email = "a@b.c")
        )

        assertEquals("""{"email":"a@b.c"}""", encoded)
    }

    @Test
    fun `the reset body carries only the token and the new password`() {
        val encoded = json.encodeToString(
            ResetPasswordRequestDto.serializer(),
            ResetPasswordRequestDto(token = "tok-1", newPassword = "Secret1Abcdefg")
        )

        assertEquals("""{"token":"tok-1","newPassword":"Secret1Abcdefg"}""", encoded)
    }
}
