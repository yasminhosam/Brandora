package com.yasminhosam.brandora.feature.auth.domain.usecase

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.validation.normalizeEmail
import javax.inject.Inject

/**
 * Posts the normalized email to /auth/forgot-password. The 200 reply is deliberately
 * ignored: the same generic success copy is shown for known and unknown addresses.
 */
class ForgotPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): ApiResult<Unit> =
        repository.forgotPassword(normalizeEmail(email))
}
