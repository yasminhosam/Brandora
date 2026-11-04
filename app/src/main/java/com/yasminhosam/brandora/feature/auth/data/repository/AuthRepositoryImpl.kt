package com.yasminhosam.brandora.feature.auth.data.repository

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.AuthErrorCodes
import com.yasminhosam.brandora.core.network.SessionExpiredSignal
import com.yasminhosam.brandora.feature.auth.data.remote.AuthRemoteDataSource
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import javax.inject.Inject

/**
 * Session persistence rules, the heart of the single-use refresh-token contract:
 *
 * - login/register success -> save the new session (rotated tokens) before returning;
 *                             a failed call never touches the stored session.
 * - refresh rejected       -> AUTH_REFRESH_INVALID means the token was used/revoked;
 *                             clear the session and let the caller surface Login.
 * - refresh response lost  -> NETWORK/TIMEOUT: the backend may have consumed the token
 *                             without answering, and a single-use token must never be
 *                             retried - clear the session instead of risking revocation.
 * - refresh rate-limited   -> tokens may still be valid; fail WITHOUT clearing.
 * - reset password success  -> the 204 revokes every session server-side, so the local one
 *                             is dropped too; a FAILED reset never touches the session.
 */
class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val sessionStore: SessionStore,
    private val expiredSignal: SessionExpiredSignal
) : AuthRepository {

    override suspend fun register(email: String, password: String): ApiResult<AuthSession> =
        remote.register(email, password).also { result ->
            // A rejected registration (email taken, validation) must not log anyone out.
            if (result is ApiResult.Success) sessionStore.save(result.data)
        }

    override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
        remote.login(email, password).also { result ->
            // A failed login never touches the stored session (typing a wrong password
            // while logged out, or while a session still exists, must not log the user out).
            if (result is ApiResult.Success) sessionStore.save(result.data)
        }

    override suspend fun refresh(): ApiResult<AuthSession> {
        val current = sessionStore.session.value
            ?: return ApiResult.Error(ApiErrorType.UNAUTHORIZED)

        return remote.refresh(current.refreshToken).also { result ->
            when {
                result is ApiResult.Success -> sessionStore.save(result.data)

                result.errorCodeIs(AuthErrorCodes.AUTH_REFRESH_INVALID) -> expire()

                // The response was lost: the single-use token may already be consumed.
                result.errorTypeIs(ApiErrorType.NETWORK) ||
                    result.errorTypeIs(ApiErrorType.TIMEOUT) -> expire()

                // 429 or anything else: keep the session; the tokens may still be valid.
            }
        }
    }

    override suspend fun forgotPassword(email: String): ApiResult<Unit> =
        remote.forgotPassword(email)

    override suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit> =
        remote.resetPassword(token, newPassword).also { result ->
            // Only a 204 revokes the sessions: an invalid token or a rate limit must not
            // log anyone out (and no expired-signal here - the Reset screen owns its
            // navigation, with its own message).
            if (result is ApiResult.Success) sessionStore.clear()
        }

    private suspend fun expire() {
        sessionStore.clear()
        expiredSignal.emit()
    }

    private fun ApiResult<*>.errorCodeIs(code: String): Boolean =
        this is ApiResult.Error && errorCode == code

    private fun ApiResult<*>.errorTypeIs(type: ApiErrorType): Boolean =
        this is ApiResult.Error && this.type == type
}
