package com.yasminhosam.brandora.feature.auth.domain.repository

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession

/**
 * Auth calls against the public /auth routes. login/register persist the new session on
 * success; [refresh] clears the stored session when the backend rejects the refresh token
 * or the refresh response is lost (the single-use token must never be retried).
 * [resetPassword] clears the session on success: a 204 revokes every session server-side.
 */
interface AuthRepository {
    suspend fun register(email: String, password: String): ApiResult<AuthSession>
    suspend fun login(email: String, password: String): ApiResult<AuthSession>
    suspend fun refresh(): ApiResult<AuthSession>
    suspend fun forgotPassword(email: String): ApiResult<Unit>
    suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit>
}
