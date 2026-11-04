package com.yasminhosam.brandora.feature.auth.domain.usecase

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.validation.normalizeEmail
import javax.inject.Inject

/** Posts the normalized email to /auth/login; the session is persisted by the repository. */
class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): ApiResult<AuthSession> =
        repository.login(normalizeEmail(email), password)
}
