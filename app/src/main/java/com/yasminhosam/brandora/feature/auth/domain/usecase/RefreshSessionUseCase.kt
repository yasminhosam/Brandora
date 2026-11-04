package com.yasminhosam.brandora.feature.auth.domain.usecase

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Exchanges the stored refresh token for a new session. The refresh token is single-use on
 * the backend, so callers MUST serialize their calls - the single-flight guarantee lives in
 * SessionRefresher (core/network), which is the only caller of this use case.
 */
class RefreshSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): ApiResult<AuthSession> = repository.refresh()
}
