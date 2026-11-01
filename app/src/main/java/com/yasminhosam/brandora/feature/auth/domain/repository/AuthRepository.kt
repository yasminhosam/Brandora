package com.yasminhosam.brandora.feature.auth.domain.repository

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession

/**
 * Auth calls against the public /auth routes. login/register persist the new session on
 * success; [refresh] clears the stored session when the backend rejects the refresh token
 * or the refresh response is lost (the single-use token must never be retried).
 */
interface AuthRepository {
    suspend fun register(email: String, password: String): ApiResult<AuthSession>
    suspend fun login(email: String, password: String): ApiResult<AuthSession>
    suspend fun refresh(): ApiResult<AuthSession>
}
